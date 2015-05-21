package com.cluda.coinsignals.streams.service

import akka.http.scaladsl.model.StatusCodes._
import awscala._
import awscala.dynamodbv2.DynamoDB

class PostSpec extends TestService {

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
      assert(respons.contains("btcaddress"))

    }
  }

  it should "responds with 'Accepted' and return the new stream object when a new signal is posted for an existing stream" in {
    Post("/streams/btcaddress/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 234.453,
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
        |  "price": 234.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": 0
        |},
        |{
        |  "timestamp": 1432122282747,
        |  "price": 234.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": -1
        |}]""".stripMargin
    ) ~> routes ~> check {
      status shouldBe Accepted
      val respons = responseAs[String]
      assert(respons.contains("btcaddress"))
    }
  }

  it should "responds with 'No Content' when posting a signal to a stream that does not exist" in {
    Post("/streams/notexisting/signals",
      """[{
        |  "timestamp": 1432122282747,
        |  "price": 234.453,
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
        |  "price": 234.453,
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

  override def afterAll(): Unit = {
    implicit val dynamoDB = DynamoDB.at(Region.US_WEST_2)

    if (dynamoDB.table(streamsTableName).isDefined) {
      dynamoDB.table(streamsTableName).get.destroy()
    }
  }

}

