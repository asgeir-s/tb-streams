package com.cluda.coinsignals.streams.messaging.postsignal

import akka.testkit.{TestActorRef, TestProbe}
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.cluda.coinsignals.streams.messaging.MessagingTest
import com.cluda.coinsignals.streams.postsignal.CalculateStatsActor
import com.cluda.coinsignals.streams.protocoll.NewStream
import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.cluda.coinsignals.streams.{DatabaseTestUtil, TestData}
import com.cluda.coinsignals.streams.model.SStream

class CalculateStatsActorTest extends MessagingTest {

  val testTableName = "calculateStatsActorTest"
  val testStreamName = "calculateStatsActorTestStream"

  override def beforeAll(): Unit = {
    val database = DynamoDB.at(Region.US_WEST_2)
    val table = DatabaseTestUtil.createStreamsTable(database, testTableName)
    DatabaseUtil.putNewStream(database, table, NewStream("calculateStatsActorTestStream", "bitstamp", "btcUSD", "secret"))
  }

  "when it receives a signal it" should
    "calculate the 'Stream.Stats' and respond with the 'Stream'" in {
    val asker = TestProbe()
    val actor = TestActorRef(CalculateStatsActor.props(testStreamName, testTableName), "calculateStatsActor1")
    asker.send(actor, TestData.signal1)
    val responds = asker.expectMsgType[SStream]
  }

}
