package com.cluda.coinsignals.streams.unit

import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.cluda.coinsignals.streams.{DatabaseTestUtil, TestData}
import com.typesafe.config.ConfigFactory

class DatabaseUtilTest extends UnitTest {

  val tableName = "databaseUtilTest"
  val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  val testDatabase = DatabaseTestUtil.createStreamsTable(dynamoDB, tableName)

  "when putting a stream and getting it abck from the database the stream" should
    "be the same" in {
    val idBack = DatabaseUtil.putStream(dynamoDB, testDatabase, TestData.adoptedStream).id
    assert(idBack == TestData.adoptedStream.id)
    val streamBack = DatabaseUtil.getStream(dynamoDB, testDatabase, idBack)
    assert(streamBack.isDefined)
    assert(streamBack.get == TestData.adoptedStream)
  }

  "when putting a stream and getting it abck from the database the stream for a new stream" should
    "be the same" in {
    val idBack = DatabaseUtil.putStream(dynamoDB, testDatabase, TestData.freshStream).id
    assert(idBack == TestData.freshStream.id)
    val streamBack = DatabaseUtil.getStream(dynamoDB, testDatabase, idBack)
    assert(streamBack.isDefined)
    assert(streamBack.get == TestData.freshStream)
  }



}
