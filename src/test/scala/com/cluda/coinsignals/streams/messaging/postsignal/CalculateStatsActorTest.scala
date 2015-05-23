package com.cluda.coinsignals.streams.messaging.postsignal

import akka.testkit.{TestActorRef, TestProbe}
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.cluda.coinsignals.streams.messaging.MessagingTest
import com.cluda.coinsignals.streams.postsignal.CalculateStatsActor
import com.cluda.coinsignals.streams.protocoll.NewStream
import com.cluda.coinsignals.streams.util.{StreamUtil, DatabaseUtil}
import com.cluda.coinsignals.streams.{DatabaseTestUtil, TestData}
import com.cluda.coinsignals.streams.model.SStream

class CalculateStatsActorTest extends MessagingTest {

  val testTableName = "calculateStatsActorTest"
  val testStreamName = "calculateStatsActorTestStream"

  override def beforeAll(): Unit = {
    val database = DynamoDB.at(Region.US_WEST_2)
    val table = DatabaseTestUtil.createStreamsTable(database, testTableName)
    DatabaseUtil.putNewStream(database, table, NewStream(testStreamName, "bitstamp", "btcUSD", "secret"))
  }

  "when it receives a signal it" should
    "calculate the 'Stream.Stats' and respond with the 'Stream'" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName), "calculateStatsActor1")
    val asker = TestProbe()
    asker.send(actor, Seq(TestData.signalSeqMath(5)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEquality(responds, TestData.mathStream2actor))

  }


  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for one new signal" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName), "calculateStatsActor1")
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(4)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEquality(responds, TestData.mathStream3actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for multiple new signals" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName), "calculateStatsActor1")
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(3), TestData.signalSeqMath(2), TestData.signalSeqMath(1)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEquality(responds, TestData.mathStream6actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for one new signal (2)" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName), "calculateStatsActor1")
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(0)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEquality(responds, TestData.mathStream7actor))
  }


    /*
   val newSStream1 = StreamUtil.updateStreamWitheNewSignal(TestData.mathStream1, TestData.signalSeqMath(5))
   assert(StreamUtil.checkRoundedEquality(newSStream1, TestData.mathStream2))

   val newSStream2 = StreamUtil.updateStreamWitheNewSignal(newSStream1, TestData.signalSeqMath(4))
   assert(StreamUtil.checkRoundedEquality(newSStream2, TestData.mathStream3))

   val newSStream3 = StreamUtil.updateStreamWitheNewSignal(newSStream2, TestData.signalSeqMath(3))
   assert(StreamUtil.checkRoundedEquality(newSStream3, TestData.mathStream4))

   val newSStream4 = StreamUtil.updateStreamWitheNewSignal(newSStream3, TestData.signalSeqMath(2))
   assert(StreamUtil.checkRoundedEquality(newSStream4, TestData.mathStream5))

   val newSStream5 = StreamUtil.updateStreamWitheNewSignal(newSStream4, TestData.signalSeqMath(1))
   assert(StreamUtil.checkRoundedEquality(newSStream5, TestData.mathStream6))

   val newSStream6 = StreamUtil.updateStreamWitheNewSignal(newSStream5, TestData.signalSeqMath(0))
   assert(StreamUtil.checkRoundedEquality(newSStream6, TestData.mathStream7))
*/


}
