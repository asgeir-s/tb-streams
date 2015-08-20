package com.cluda.coinsignals.streams.service

import akka.http.impl.util.JavaMapping.HttpHeader
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.amazonaws.regions.Region
import com.amazonaws.services.sns.model.DeleteTopicRequest
import com.cluda.coinsignals.protocol.Sec
import com.typesafe.config.ConfigFactory

class FullServiceSpec extends TestService {

  override val streamsTableName: String = "postStreamSpec"

  it should "responds accept the new stream and return the id" in {
    Post("/streams",
      Sec.secureMessage(
      """{
        |"id": "coinbase-account-id",
        |"exchange": "bitstamp",
        |"currencyPair": "btcUSD",
        |"payoutAddress": "publishers-bitcoin-address",
        |"subscriptionPriceUSD": 5
        |}""".stripMargin)
    ).addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe Accepted
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
      println(respons)
      assert(respons.contains("id"))
      assert(respons.contains("apiKey"))
    }
  }


  it should "responds with 'Accepted' and return the new stream object when a new signal is posted for an existing stream" in {
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody(
        """[{
          |  "timestamp": 1432122282747,
          |  "price": 200.453,
          |  "change": 0,
          |  "id": 1,
          |  "value": 100,
          |  "signal": 1
          |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Accepted
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
      assert(respons.contains("coinbase-account-id"))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when two new signals is posted for an existing stream" in {
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody("""[{
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
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Accepted
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
      assert(respons.contains("coinbase-account-id"))
    }
  }

  it should "responds with 'Accepted' and return the new stream object when three signals is posted ina out of order sequence" in {
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody("""[{
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
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Accepted
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
      assert(respons.contains("coinbase-account-id"))
    }
  }

  it should "responds with 'NotAcceptable' and return with a error without writing anything to the database when " +
    "signals are in a illegal order(correct id but no CLOSE between LONG and SHORT positions), also if new signals are " +
    "received later with the same id's but correct order thay should be acceted" in {
    // illegal
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody("""[{
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
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe NotAcceptable
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
      assert(respons.contains("invalid sequence of signals"))
    }
    // Correct
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody("""[{
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
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Accepted
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
      assert(respons.contains("coinbase-account-id"))
    }
  }

  it should "responds with 'NotAcceptable' when the id has te expected next id but the posting is the same as the last position" in {
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody("""[{
        |  "timestamp": 1432122282747,
        |  "price": 214.453,
        |  "change": 0,
        |  "id": 10,
        |  "value": 100,
        |  "signal": 1
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe NotAcceptable
    }
  }

  it should "responds with 'No Content' when posting a signal to a stream that does not exist" in {
    Post("/streams/notexisting/signals",
      postAwsSnsBody("""[{
        |  "timestamp": 1432122282747,
        |  "price": 274.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": 1
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe NoContent
    }
  }

  it should "responds with 'Conflict' when a signal that is the same as last signal is added. The responds body should include the stream object for the stream" in {
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody("""[{
        |  "timestamp": 1432122282747,
        |  "price": 284.453,
        |  "change": 0,
        |  "id": 1,
        |  "value": 100,
        |  "signal": -1
        |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Conflict
      val respons = Sec.validateAndDecryptMessage(responseAs[String]).get
    }
  }


  it should "be possible to get the stream info as json with no secrets" in {
    Get("/streams/coinbase-account-id").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe OK
      val responsRaw = responseAs[String]
      val respons = Sec.validateAndDecryptMessage(responsRaw).get
      assert(respons.contains("coinbase-account-id"))
      assert(!respons.contains("topicArn"))
      assert(!respons.contains("apiKey"))
    }
  }

  it should "be possible to get the stream info as json with apiKey and topicArn" in {
    Get("/streams/coinbase-account-id?private=true").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe OK
      val responsRaw = responseAs[String]
      val respons = Sec.validateAndDecryptMessage(responsRaw).get
      assert(respons.contains("coinbase-account-id"))
      assert(respons.contains("topicArn"))
      assert(respons.contains("apiKey"))
    }
  }

  it should "respondse with NoContent when trying to retreive a stream that does not exist" in {
    Get("/streams/fackestream?private=true").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe NotFound
    }

    Get("/streams/fackestream").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe NotFound
    }

  }

  it should "handle AWS SNS subscription messages" in {
    // putted in a lot of crap in the body to se it finds the correct key value pare the interacting value is 'SubscribeURL'
    Post("/streams/coinbase-account-id/signals",
      """{
      "Type" : "Notification",
      "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
      "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
      "SubscribeURL": "https://www.msn.com/nb-no/underholdning/nyheter/her-f%C3%A5r-enrique-iglesias-kuttet-fingrene-p%C3%A5-scenen/ar-BBktl44",
      "Subject" : "My First Message",
      "Message" : "[{\n  \"timestamp\": 1433169808000,\n  \"price\": 227.5100,\n  \"change\": 0E-10,\n  \"id\": 3,\n  \"value\": 1.0000000000,\n  \"signal\": 0\n}]",
      "Timestamp" : "2012-05-02T00:54:06.655Z",
      "SignatureVersion" : "1",
      "Signature" : "EXAMPLEw6JRNwm1LFQL4ICB0bnXrdB8ClRMTQFGBqwLpGbM78tJ4etTwC5zU7O3tS6tGpey3ejedNdOJ+1fkIp9F2/LmNVKb5aFlYq+9rk9ZiPph5YlLmWsDcyC5T+Sy9/umic5S0UQc2PEtgdpVBahwNOdMW4JPwk0kAJJztnc=",
      "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
      "UnsubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96"
    }""").addHeader(RawHeader("x-amz-sns-message-type","SubscriptionConfirmation")) ~> routes ~> check {
      status shouldBe OK
    }
  }

  it should "handle new signals incoming as AWS SNS messages" in {
    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody(
        """[{
          |"timestamp":1433169808000,
          |"price":227.5100,
          |"change":0E-10,
          |"id":10,
          |"value": 1.0000000000,
          |"signal": 0
          |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Accepted
    }

    Post("/streams/coinbase-account-id/signals",
      postAwsSnsBody(
        """[{
          |"timestamp":1433169808000,
          |"price":227.5100,
          |"change":0E-10,
          |"id":11,
          |"value": 1.0000000000,
          |"signal": 1
          |}]""".stripMargin)
    ).addHeader(RawHeader("x-amz-sns-message-type","Notification")) ~> routes ~> check {
      status shouldBe Accepted
    }

  }

  it should "be possible to get all streams" in {
    Get("/streams").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe OK
      val responsRaw = responseAs[String]
      val respons = Sec.validateAndDecryptMessage(responsRaw).get
      respons.contains("coinbase-account-id")
    }
  }

  it should "be possible to change the subscription price" in {
    Post("/streams/coinbase-account-id/subscription-price", Sec.secureMessage("4.66")).addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe Accepted
    }

    Get("/streams/coinbase-account-id").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe OK
      val responsRaw = responseAs[String]
      val respons = Sec.validateAndDecryptMessage(responsRaw).get
      assert(respons.contains("4.66"))
    }

    Post("/streams/coinbase-account-id/subscription-price", Sec.secureMessage("40.33")).addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe Accepted
    }

    Get("/streams/coinbase-account-id").addHeader(Sec.headerToken) ~> routes ~> check {
      status shouldBe OK
      val responsRaw = responseAs[String]
      val respons = Sec.validateAndDecryptMessage(responsRaw).get
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

  def postAwsSnsBody(signals: String): String = {
      """{
      "Type" : "Notification",
      "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
      "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
      "Subject" : "My First Message",
      "Message" : """" +
        Sec.secureMessage(signals) + """",
      "Timestamp" : "2012-05-02T00:54:06.655Z",
      "SignatureVersion" : "1",
      "Signature" : "EXAMPLEw6JRNwm1LFQL4ICB0bnXrdB8ClRMTQFGBqwLpGbM78tJ4etTwC5zU7O3tS6tGpey3ejedNdOJ+1fkIp9F2/LmNVKb5aFlYq+9rk9ZiPph5YlLmWsDcyC5T+Sy9/umic5S0UQc2PEtgdpVBahwNOdMW4JPwk0kAJJztnc=",
      "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
      "UnsubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96"
  }"""
  }

}

