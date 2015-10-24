package com.cluda.tradersbit.streams.messaging.postsignal

import java.util.UUID

import akka.http.scaladsl.model.headers.RawHeader
import akka.testkit.{TestActorRef, TestProbe}
import com.amazonaws.regions.Region
import com.cluda.tradersbit.streams.model.Signal
import com.cluda.tradersbit.streams.util.StreamUtil
import com.cluda.tradersbit.streams.TestData
import com.cluda.tradersbit.streams.util.{StreamUtil, DatabaseUtil}
import com.cluda.tradersbit.streams.{TestData, DatabaseTestUtil}
import com.cluda.tradersbit.streams.messaging.MessagingTest
import com.cluda.tradersbit.streams.model.{Signal, SStream}
import com.cluda.tradersbit.streams.postsignal.CalculateStatsActor
import com.cluda.tradersbit.streams.protocoll.UnexpectedSignalException
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class CalculateStatsActorTest extends MessagingTest {

  val testTableName = "calculateStatsActorTest"
  var testStreamId = ""
  def globalRequestID() = UUID.randomUUID().toString

  override def beforeAll(): Unit = {
    implicit val database = DatabaseUtil.awscalaDB(ConfigFactory.load())
    val table = DatabaseTestUtil.createStreamsTable(database, testTableName)
    testStreamId = Await.result(DatabaseUtil.putStreamNew(table, TestData.freshStream), 5 seconds).id.get
    Await.result(DatabaseUtil.addSnsTopicArn(table, testStreamId, "topicARN"), 5 seconds)
  }

  "when it receives a signal it" should
    "calculate the 'Stream.Stats' and respond with the 'Stream'" in {
    val actor = TestActorRef(CalculateStatsActor.props(globalRequestID(), testStreamId, testTableName))
    val asker = TestProbe()
    asker.send(actor, Seq(TestData.signalSeqMath(5)))
    val responds = asker.expectMsgType[SStream]
    //println("responds stream: " + responds.privateJson)
    //println(TestData.mathStream2actor)
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(responds, TestData.mathStream2actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for one new signal" in {
    val actor = TestActorRef(CalculateStatsActor.props(globalRequestID(), testStreamId, testTableName))
    val asker = TestProbe()
    asker.send(actor, Seq(TestData.signalSeqMath(4)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(responds, TestData.mathStream3actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for multiple new signals" in {
    val actor = TestActorRef(CalculateStatsActor.props(globalRequestID(), testStreamId, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(3), TestData.signalSeqMath(2), TestData.signalSeqMath(1)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(responds, TestData.mathStream6actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for one new signal (2)" in {
    val actor = TestActorRef(CalculateStatsActor.props(globalRequestID(), testStreamId, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(0)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(responds, TestData.mathStream7actor))
  }

  "when it receive a signal that has a id smaller then the last processed signal it" should
    "ignore the signal and send beack 'UnexpectedSignalException'" in {
    val actor = TestActorRef(CalculateStatsActor.props(globalRequestID(), testStreamId, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(3)))
    val responds = asker.expectMsgType[UnexpectedSignalException]
    assert(responds.info.contains("already been processed"))
  }

  "when it receive a signal that has a id lager then the next expected id (lastId + 1) it" should
    "create a MissingSignalsActor and send the id of the last processed signal to it" in {
    val actor = TestActorRef(CalculateStatsActor.props(globalRequestID(), testStreamId, testTableName))
    val underlyingActor = actor.underlyingActor.asInstanceOf[CalculateStatsActor]
    val asker = TestProbe()

    assert(underlyingActor.context.children.isEmpty)
    asker.send(actor, Seq(Signal(8, 1, TestData.timestamp + 10, BigDecimal(975), BigDecimal(0.5), BigDecimal(1.5))))
    assert(underlyingActor.context.children.size == 1)
  }

  override def afterAll(): Unit = {
    implicit val database = DatabaseUtil.awscalaDB(ConfigFactory.load())
    val table = DatabaseTestUtil.createStreamsTable(database, testTableName)
    DatabaseUtil.removeStream(table, testStreamId)
    super.afterAll()
  }

}
