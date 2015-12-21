package com.cluda.tradersbit.streams.service

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import com.amazonaws.regions.Region
import com.cluda.tradersbit.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class FullServiceSpec extends TestService {

  override val streamsTableName: String = "postStreamSpec"
  var streamId1 = ""
  var streamId2 = ""
  def globalRequestIDHeader() = RawHeader("Global-Request-ID", UUID.randomUUID().toString)


  it should "responds accept the new stream and return the id" in {
    import spray.json._

    val randomName1 = UUID.randomUUID().toString.substring(0, 11)
    val randomName2 = UUID.randomUUID().toString.substring(0, 11)

    Post("/streams",
      s"""{
        | "name": "$randomName1",
        | "exchange": "bitfinex",
        | "currencyPair": "btcUSD",
        | "payoutAddress": "publishers-bitcoin-address",
        | "subscriptionPriceUSD": 5
        |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains("id"))
      assert(!respons.contains("apiKeyId"))
      streamId1 = respons.parseJson.asJsObject.fields("id").toString()
      streamId1 = streamId1.substring(1, streamId1.length-1)
    }


    Post("/streams",
      s"""{
        | "name": "$randomName2",
        | "exchange": "bitfinex",
        | "currencyPair": "btcUSD",
        | "payoutAddress": "publishers-bitcoin-address",
        | "subscriptionPriceUSD": 10
        |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains("id"))
      assert(!respons.contains("apiKeyId"))
      streamId2 = respons.parseJson.asJsObject.fields("id").toString()
      streamId2 = streamId2.substring(1, streamId2.length-1)
    }
  }

  it should "not accept two streams with the same name" in {
    import spray.json._

    val randomName1 = UUID.randomUUID().toString.substring(0, 11)

    Post("/streams",
      s"""{
          | "name": "$randomName1",
          | "exchange": "bitfinex",
          | "currencyPair": "btcUSD",
          | "payoutAddress": "publishers-bitcoin-address",
          | "subscriptionPriceUSD": 5
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains("id"))
      assert(!respons.contains("apiKeyId"))
      streamId1 = respons.parseJson.asJsObject.fields("id").toString()
      streamId1 = streamId1.substring(1, streamId1.length-1)
    }

    Post("/streams",
      s"""{
          | "name": "$randomName1",
          | "exchange": "bitfinex",
          | "currencyPair": "btcUSD",
          | "payoutAddress": "publishers-bitcoin-address",
          | "subscriptionPriceUSD": 10
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe InternalServerError
      val respons = responseAs[String]
      println(respons)
    }
  }



  it should "responds with 'Accepted' and return the new stream object when a new signal is posted for an existing stream" in {
    Post(s"/streams/$streamId1/signals",
        """[{
          |  "timestamp": 1432122282747,
          |  "price": 200.453,
          |  "change": 0,
          |  "id": 1,
          |  "value": 100,
          |  "signal": 1
          |}]""".stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when two new signals is posted for an existing stream" in {
    Post(s"/streams/$streamId1/signals",
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
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when three signals is posted ina out of order sequence" in {
    Post(s"/streams/$streamId1/signals",
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
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
    }
  }

  it should "responds with 'NotAcceptable' and return with a error without writing anything to the database when " +
    "signals are in a illegal order(correct id but no CLOSE between LONG and SHORT positions), also if new signals are " +
    "received later with the same id's but correct order thay should be acceted" in {
    // illegal
    Post(s"/streams/$streamId1/signals",
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
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe NotAcceptable
      val respons = responseAs[String]
      assert(respons.contains("Invalid sequence of signals"))
    }
    // Correct
    Post(s"/streams/$streamId1/signals",
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
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
    }
  }

  it should "responds with 'NotAcceptable' when the id has the expected next id but the posting is the same as the last position" in {
    Post(s"/streams/$streamId1/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 214.453,
        |  "change": 0,
        |  "id": 10,
        |  "value": 100,
        |  "signal": 1
        |}]""".stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
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
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe NoContent
    }
  }

  it should "responds with 'Conflict' when a signal that is the same as last signal is added. The responds body should include the stream object for the stream" in {
    Post(s"/streams/$streamId1/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 284.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": -1
        |}]""".stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Conflict
      val respons = responseAs[String]
    }
  }


  it should "be possible to get the stream info as json with no secrets" in {
    Post("/streams/get",
      s"""
        |{
        |   "streams": ["$streamId1"]
        |}
      """.stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
      assert(respons.contains("name"))
      assert(!respons.contains("status"))
    }
  }

  it should "be possible to get the stream info as json with auth level info" in {
    Post(s"/streams/get",
      s"""
        |{
        |   "streams": ["$streamId1"],
        |   "infoLevel": "auth"
        |}
      """.stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
      assert(respons.contains("name"))
      assert(respons.contains("status"))
    }
  }

  it should "respondse with NoContent when trying to retreive a stream that does not exist" in {
    Post("/streams/get",
      s"""
        |{
        |   "streams": ["fackestream"],
        |   "infoLevel": "auth"
        |}
      """.stripMargin
      ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe NotFound
    }

    Post("/streams/get",
      s"""
         |{
         |   "streams": ["fackestream"]
         |}
      """.stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe NotFound
    }

  }

  it should "handle new signals incoming as AWS SNS messages" in {
    Post(s"/streams/$streamId1/signals",

        """[{
          |"timestamp":1433169808000,
          |"price":227.5100,
          |"change":0E-10,
          |"id":10,
          |"value": 1.0000000000,
          |"signal": 0
          |}]""".stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
    }

    Post(s"/streams/$streamId1/signals",

        """[{
          |"timestamp":1433169808000,
          |"price":227.5100,
          |"change":0E-10,
          |"id":11,
          |"value": 1.0000000000,
          |"signal": 1
          |}]""".stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
    }

  }

  it should "be possible to get all streams" in {
    Get("/streams").addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains("name"))
      respons.contains(streamId1)
    }
  }

  it should "be possible to change the subscription price" in {
    Post(s"/streams/$streamId1/subscription-price", "4.66").addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
    }

    Post(s"/streams/get",
      s"""
         |{
         |   "streams": ["$streamId1"]
         |}
      """.stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains("4.66"))
    }

    Post(s"/streams/$streamId1/subscription-price", "40.33").addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe Accepted
    }

    Post(s"/streams/get",
      s"""
         |{
         |   "streams": ["$streamId1"]
         |}
      """.stripMargin
    ).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(!respons.contains("4.66"))
      assert(respons.contains("40.33"))
    }

  }

  it should "be possible to get requested streams's with public info level" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1"],
          | "infoLevel": "public"
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(!respons.contains("status"))
      assert(respons.contains(streamId1))

    }
  }

  it should "return the public info for the stream when the level field is missing" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1"]
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(!respons.contains("status"))
      assert(respons.contains(streamId1))

    }
  }

  it should "be possible to get requested streams's auth level info" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1"],
          | "infoLevel": "auth"
          |
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains("status"))
      assert(respons.contains(streamId1))
      assert(respons.startsWith("["))
    }
  }


  it should "be possible to get multiple requested streams with auth level info" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1", "$streamId2"],
          | "infoLevel": "auth"
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]

      assert(respons.contains("status"))
      assert(respons.contains(streamId1))
      assert(respons.contains(streamId2))
    }
  }

  it should "be possible to get multiple requested streams with public info level" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1", "$streamId2"],
          | "infoLevel": "public"
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]

      assert(!respons.contains("status"))
      assert(respons.contains(streamId1))
      assert(respons.contains(streamId2))
      assert(respons.startsWith("["))
    }
  }

  it should "be possible to get multiple requested streams with auth info level" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1", "$streamId2"],
          | "infoLevel": "auth"
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]

      assert(respons.contains("status"))
      assert(!respons.contains("topicArn"))
      assert(respons.contains(streamId1))
      assert(respons.contains(streamId2))
      assert(respons.startsWith("["))
    }
  }

  it should "be possible to get multiple requested streams with private info level" in {
    import spray.json._

    Post("/streams/get",
      s"""{
          | "streams": ["$streamId1", "$streamId2"],
          | "infoLevel": "private"
          |}""".stripMargin).addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]

      assert(respons.contains("status"))
      assert(respons.contains("topicArn"))
      assert(respons.contains(streamId1))
      assert(respons.contains(streamId2))
      assert(respons.startsWith("["))
    }
  }

  it should "for a single stream be possible to do a GET request to get the stream info as json without private info" in {
    Get(s"/streams/$streamId1").addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
      assert(!respons.contains("status"))
      assert(respons.startsWith("{"))
    }
  }

  it should "for a single stream be possible to do a GET request to get the stream info as json private info" in {
    Get(s"/streams/$streamId1?private=true").addHeader(globalRequestIDHeader) ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains(streamId1))
      assert(respons.contains("status"))
      assert(respons.startsWith("{"))
    }
  }


  override def afterAll(): Unit = {
    implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())

    if (dynamoDB.table(streamsTableName).isDefined) {
      dynamoDB.table(streamsTableName).get.destroy()
    }
  }

}

