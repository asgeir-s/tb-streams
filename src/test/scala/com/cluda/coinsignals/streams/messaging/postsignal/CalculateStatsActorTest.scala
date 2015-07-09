package com.cluda.coinsignals.streams.messaging.postsignal

import akka.testkit.{TestActorRef, TestProbe}
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.amazonaws.regions.Region
import com.cluda.coinsignals.streams.messaging.MessagingTest
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.postsignal.CalculateStatsActor
import com.cluda.coinsignals.streams.protocoll.{NewStream, UnexpectedSignalException}
import com.cluda.coinsignals.streams.util.{DatabaseUtil, StreamUtil}
import com.cluda.coinsignals.streams.{DatabaseTestUtil, TestData}
import com.typesafe.config.ConfigFactory

class CalculateStatsActorTest extends MessagingTest {

  val testTableName = "calculateStatsActorTest"
  val testStreamName = "calculateStatsActorTestStream"
  val testStreamApiKey = "key"

  val config = ConfigFactory.load()
  implicit val region: Region = awscala.Region.US_WEST_2
  val awscalaCredentials = awscala.BasicCredentialsProvider(
    config.getString("aws.accessKeyId"),
    config.getString("aws.secretAccessKey"))

  override def beforeAll(): Unit = {
    val database = awscala.dynamodbv2.DynamoDB(awscalaCredentials)
    val table = DatabaseTestUtil.createStreamsTable(database, testTableName)
    DatabaseUtil.putNewStream(database, table, NewStream(testStreamName, "bitstamp", "btcUSD", "btcAddress", 10), "topicARN", testStreamApiKey)
  }

  "when it receives a signal it" should
    "calculate the 'Stream.Stats' and respond with the 'Stream'" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName))
    val asker = TestProbe()
    asker.send(actor, Seq(TestData.signalSeqMath(5)))
    val responds = asker.expectMsgType[SStream]
    println(responds)
    println(TestData.mathStream2actor)
    assert(StreamUtil.checkRoundedEqualityExceptApiKey(responds, TestData.mathStream2actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for one new signal" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(4)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEqualityExceptApiKey(responds, TestData.mathStream3actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for multiple new signals" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(3), TestData.signalSeqMath(2), TestData.signalSeqMath(1)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEqualityExceptApiKey(responds, TestData.mathStream6actor))
  }

  "[math test] CalculateStatsActor" should
    "write correctly updated stats to the database and return the new 'SStream' for one new signal (2)" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(0)))
    val responds = asker.expectMsgType[SStream]
    assert(StreamUtil.checkRoundedEqualityExceptApiKey(responds, TestData.mathStream7actor))
  }

  "when it receive a signal that has a id smaller then the last processed signal it" should
    "ignore the signal and send beack 'UnexpectedSignalException'" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName))
    val asker = TestProbe()

    asker.send(actor, Seq(TestData.signalSeqMath(3)))
    val responds = asker.expectMsgType[UnexpectedSignalException]
    assert(responds.info.contains("already been processed"))
  }

  "when it receive a signal that has a id lager then the next expected id (lastId + 1) it" should
    "create a MissingSignalsActor and send the id of the last processed signal to it" in {
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName))
    val underlyingActor = actor.underlyingActor.asInstanceOf[CalculateStatsActor]
    val asker = TestProbe()

    assert(underlyingActor.context.children.isEmpty)
    asker.send(actor, Seq(Signal(8, 1, TestData.timestamp + 10, BigDecimal(975), BigDecimal(0.5), BigDecimal(1.5))))
    assert(underlyingActor.context.children.size == 1)
  }


}
