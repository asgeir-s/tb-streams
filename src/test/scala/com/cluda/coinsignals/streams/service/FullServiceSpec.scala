package com.cluda.coinsignals.streams.service

import akka.http.scaladsl.model.StatusCodes._
import com.amazonaws.regions.Region
import com.typesafe.config.ConfigFactory

class FullServiceSpec extends TestService {

  override val streamsTableName: String = "postStreamSpec"
  var streamId = ""

  it should "responds accept the new stream and return the id" in {
    import spray.json._

    Post("/streams",
      """{
        | "exchange": "bitstamp",
        | "currencyPair": "btcUSD",
        | "payoutAddress": "publishers-bitcoin-address",
        | "subscriptionPriceUSD": 5
        |}""".stripMargin) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains("id"))
      assert(respons.contains("apiKeyId"))
      streamId = respons.parseJson.asJsObject.fields("id").toString()
      streamId = streamId.substring(1, streamId.length-1)
    }
  }



  it should "responds with 'Accepted' and return the new stream object when a new signal is posted for an existing stream" in {
    Post(s"/streams/$streamId/signals",
        """[{
          |  "timestamp": 1432122282747,
          |  "price": 200.453,
          |  "change": 0,
          |  "id": 1,
          |  "value": 100,
          |  "signal": 1
          |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when two new signals is posted for an existing stream" in {
    Post(s"/streams/$streamId/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 254.453,
        |  "change": 0,
        |  "id": 2,
        |  "value": 100,
        |  "signal": 0
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 294.453,
        |  "change": 0,
        |  "id": 3,
        |  "value": 100,
        |  "signal": -1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when three signals is posted ina out of order sequence" in {
    Post(s"/streams/$streamId/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 264.453,
        |  "change": 0,
        |  "id": 6,
        |  "value": 100,
        |  "signal": 0
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 234.453,
        |  "change": 0,
        |  "id": 4,
        |  "value": 100,
        |  "signal": 0
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 214.453,
        |  "change": 0,
        |  "id": 5,
        |  "value": 100,
        |  "signal": -1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId))
    }
  }

  it should "responds with 'NotAcceptable' and return with a error without writing anything to the database when " +
    "signals are in a illegal order(correct id but no CLOSE between LONG and SHORT positions), also if new signals are " +
    "received later with the same id's but correct order thay should be acceted" in {
    // illegal
    Post(s"/streams/$streamId/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 244.453,
        |  "change": 0,
        |  "id": 7,
        |  "value": 100,
        |  "signal": -1
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 294.453,
        |  "change": 0,
        |  "id": 8,
        |  "value": 100,
        |  "signal": 1
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 204.453,
        |  "change": 0,
        |  "id": 9,
        |  "value": 100,
        |  "signal": 0
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe NotAcceptable
      val respons = responseAs[String]
      assert(respons.contains("invalid sequence of signals"))
    }
    // Correct
    Post(s"/streams/$streamId/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 214.453,
        |  "change": 0,
        |  "id": 7,
        |  "value": 100,
        |  "signal": -1
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 254.453,
        |  "change": 0,
        |  "id": 8,
        |  "value": 100,
        |  "signal": 0
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 284.453,
        |  "change": 0,
        |  "id": 9,
        |  "value": 100,
        |  "signal": 1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId))
    }
  }

  it should "responds with 'NotAcceptable' when the id has te expected next id but the posting is the same as the last position" in {
    Post(s"/streams/$streamId/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 214.453,
        |  "change": 0,
        |  "id": 10,
        |  "value": 100,
        |  "signal": 1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe NotAcceptable
    }
  }

  it should "responds with 'No Content' when posting a signal to a stream that does not exist" in {
    Post("/streams/notexisting/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 274.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": 1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe NoContent
    }
  }

  it should "responds with 'Conflict' when a signal that is the same as last signal is added. The responds body should include the stream object for the stream" in {
    Post(s"/streams/$streamId/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 284.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": -1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Conflict
      val respons = responseAs[String]
    }
  }


  it should "be possible to get the stream info as json with no secrets" in {
    Get(s"/streams/$streamId") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains(streamId))
      assert(!respons.contains("topicArn"))
      assert(!respons.contains("apiKey"))
    }
  }

  it should "be possible to get the stream info as json with apiKey and topicArn" in {
    Get(s"/streams/$streamId?private=true") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains(streamId))
      assert(respons.contains("topicArn"))
      assert(respons.contains("apiKey"))
    }
  }

  it should "respondse with NoContent when trying to retreive a stream that does not exist" in {
    Get("/streams/fackestream?private=true") ~> routes ~> check {
      status shouldBe NotFound
    }

    Get("/streams/fackestream") ~> routes ~> check {
      status shouldBe NotFound
    }

  }

  it should "handle new signals incoming as AWS SNS messages" in {
    Post(s"/streams/$streamId/signals",

        """[{
          |"timestamp":1433169808000,
          |"price":227.5100,
          |"change":0E-10,
          |"id":10,
          |"value": 1.0000000000,
          |"signal": 0
          |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
    }

    Post(s"/streams/$streamId/signals",

        """[{
          |"timestamp":1433169808000,
          |"price":227.5100,
          |"change":0E-10,
          |"id":11,
          |"value": 1.0000000000,
          |"signal": 1
          |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
    }

  }

  it should "be possible to get all streams" in {
    Get("/streams") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      respons.contains(streamId)
    }
  }

  it should "be possible to change the subscription price" in {
    Post(s"/streams/$streamId/subscription-price", "4.66") ~> routes ~> check {
      status shouldBe Accepted
    }

    Get(s"/streams/$streamId") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains("4.66"))
    }

    Post(s"/streams/$streamId/subscription-price", "40.33") ~> routes ~> check {
      status shouldBe Accepted
    }

    Get(s"/streams/$streamId") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(!respons.contains("4.66"))
      assert(respons.contains("40.33"))
    }
  }


  override def afterAll(): Unit = {
    val config = ConfigFactory.load()
    implicit val region: Region = awscala.Region.US_WEST_2
    val awscalaCredentials = awscala.BasicCredentialsProvider(
      config.getString("aws.accessKeyId"),
      config.getString("aws.secretAccessKey"))

    implicit val dynamoDB = awscala.dynamodbv2.DynamoDB(awscalaCredentials)

    if (dynamoDB.table(streamsTableName).isDefined) {
      dynamoDB.table(streamsTableName).get.destroy()
    }
  }

}

