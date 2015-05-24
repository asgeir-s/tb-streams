package com.cluda.coinsignals.streams.messaging.postsignal

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.{TestActorRef, TestProbe}
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.cluda.coinsignals.streams.messaging.MessagingTest
import com.cluda.coinsignals.streams.postsignal.PostSignalActor
import com.cluda.coinsignals.streams.protocoll.StreamDoesNotExistException
import com.cluda.coinsignals.streams.{DatabaseTestUtil, TestData}


class PostSignalActorTest extends MessagingTest {

  val testTableName = "postSignalActorTest"

  override def beforeAll(): Unit = {
    DatabaseTestUtil.createStreamsTable(DynamoDB.at(Region.US_WEST_2), testTableName)
  }

  "when receiving a 'Signals' it" should
    "start a 'CalculateStatsActor' and become responder" in {
    val actor = TestActorRef(PostSignalActor.props("test-stream1", testTableName), "postSignalActor1")
    val underlyingActor = actor.underlyingActor.asInstanceOf[PostSignalActor]
    assert(underlyingActor.context.children.isEmpty)
    actor ! Seq(TestData.signal1)
    assert(underlyingActor.context.children.nonEmpty)
  }

  "when in responder mode and receiving 'StreamDoesNotExistException' it" should
    "respond with NoContent" in {
    val interface = TestProbe()
    val actor = TestActorRef(PostSignalActor.props("test-stream2", testTableName), "postSignalActor2")
    interface.send(actor, Seq(TestData.signal1)) // become responder
    actor ! StreamDoesNotExistException("something")
    val responds = interface.expectMsgType[HttpResponse]
    responds.status == NoContent
  }


}
