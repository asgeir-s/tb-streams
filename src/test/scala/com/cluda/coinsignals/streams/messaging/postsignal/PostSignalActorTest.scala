package com.cluda.coinsignals.streams.messaging.postsignal

import java.util.UUID

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.testkit.{TestActorRef, TestProbe}
import com.cluda.coinsignals.streams.messaging.MessagingTest
import com.cluda.coinsignals.streams.postsignal.PostSignalActor
import com.cluda.coinsignals.streams.protocoll.StreamDoesNotExistException
import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.cluda.coinsignals.streams.{DatabaseTestUtil, TestData}
import com.typesafe.config.ConfigFactory


class PostSignalActorTest extends MessagingTest {

  val testTableName = "postSignalActorTest"
  def globalRequestID() = UUID.randomUUID().toString

  override def beforeAll(): Unit = {
    DatabaseTestUtil.createStreamsTable(DatabaseUtil.awscalaDB(ConfigFactory.load()), testTableName)
  }

  "when receiving a 'Signals' it" should
    "start a 'CalculateStatsActor' and become responder" in {
    val actor = TestActorRef(PostSignalActor.props(globalRequestID(), "test-stream1", testTableName), "postSignalActor1")
    val underlyingActor = actor.underlyingActor.asInstanceOf[PostSignalActor]
    assert(underlyingActor.context.children.isEmpty)
    actor ! Seq(TestData.signal1)
    assert(underlyingActor.context.children.nonEmpty)
  }

  "when in responder mode and receiving 'StreamDoesNotExistException' it" should
    "respond with NoContent" in {
    val interface = TestProbe()
    val actor = TestActorRef(PostSignalActor.props(globalRequestID(), "test-stream2", testTableName), "postSignalActor2")
    interface.send(actor, Seq(TestData.signal1)) // become responder
    actor ! StreamDoesNotExistException("something")
    val responds = interface.expectMsgType[HttpResponse]
    responds.status == NoContent
  }


}
