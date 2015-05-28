package com.cluda.coinsignals.streams.service

import akka.http.scaladsl.model.StatusCodes._
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.amazonaws.services.sns.model.DeleteTopicRequest

class FullServiceSpec extends TestService {

  override val streamsTableName: String = "postStreamSpec"

  it should "responds accept the new stream and return the id" in {
    Post("/streams",
      """{
        |"id": "btcaddress",
        |"exchange": "bitstamp",
        |"currencyPair": "btcUSD",
        |"apiKey": "secretkeyOrWhat"
        |}""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      println(respons)
      assert(respons.contains("btcaddress"))

    }
  }

  it should "responds with 'Accepted' and return the new stream object when a new signal is posted for an existing stream" in {
    Post("/streams/btcaddress/signals",
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
      assert(respons.contains("btcaddress"))

    }
  }

  it should "responds with 'Accepted' and return the new stream object when two new signals is posted for an existing stream" in {
    Post("/streams/btcaddress/signals",
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
      assert(respons.contains("btcaddress"))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when three signals is posted ina out of order sequence" in {
    Post("/streams/btcaddress/signals",
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
      assert(respons.contains("btcaddress"))
    }
  }

  it should "responds with 'NotAcceptable' and return with a error without writing anything to the database when " +
    "signals are in a illegal order(correct id but no CLOSE between LONG and SHORT positions), also if new signals are " +
    "received later with the same id's but correct order thay should be acceted" in {
    // illegal
    Post("/streams/btcaddress/signals",
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
    Post("/streams/btcaddress/signals",
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
      val response = responseAs[String]
      assert(response.contains("btcaddress"))
    }
  }

  it should "responds with 'NotAcceptable' when the id has te expected next id but the posting is the same as the last position" in {
    Post("/streams/btcaddress/signals",
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
    Post("/streams/btcaddress/signals",
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
    Get("/streams/btcaddress") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains("btcaddress"))
      assert(!respons.contains("topicArn"))
      assert(!respons.contains("apiKey"))
    }
  }

  it should "be possible to get the stream info as json with apiKey and topicArn" in {
    Get("/streams/btcaddress?private=true") ~> routes ~> check {
      status shouldBe OK
      val respons = responseAs[String]
      assert(respons.contains("btcaddress"))
      assert(respons.contains("topicArn"))
      assert(respons.contains("apiKey"))
    }
  }


  override def afterAll(): Unit = {
    implicit val dynamoDB = DynamoDB.at(Region.US_WEST_2)

    if (dynamoDB.table(streamsTableName).isDefined) {
      dynamoDB.table(streamsTableName).get.destroy()
    }
  }

}

