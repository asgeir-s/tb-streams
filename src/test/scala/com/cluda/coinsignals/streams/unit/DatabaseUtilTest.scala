package com.cluda.coinsignals.streams.unit

import awscala._
import awscala.dynamodbv2.DynamoDB
import com.cluda.coinsignals.streams.{TestData, DatabaseTestUtil}
import com.cluda.coinsignals.streams.util.DatabaseUtil

class DatabaseUtilTest extends UnitTest {

  val tableName = "databaseUtilTest"
  val dynamoDB = DynamoDB.at(Region.US_WEST_2)
  val testDatabase = DatabaseTestUtil.createStreamsTable(dynamoDB, tableName)

  "when putting a stream and getting it abck from the database the stream" should
    "be the same" in {
    val idBack = DatabaseUtil.putStream(dynamoDB, testDatabase, TestData.adoptedStream)
    assert(idBack == TestData.adoptedStream.id)
    val streamBack = DatabaseUtil.getStream(dynamoDB, testDatabase, idBack)
    assert(streamBack isDefined)
    assert(streamBack.get == TestData.adoptedStream)
  }

  "when putting a stream and getting it abck from the database the stream for a new stream" should
    "be the same" in {
    val idBack = DatabaseUtil.putStream(dynamoDB, testDatabase, TestData.freshStream)
    assert(idBack == TestData.freshStream.id)
    val streamBack = DatabaseUtil.getStream(dynamoDB, testDatabase, idBack)
    assert(streamBack isDefined)
    assert(streamBack.get == TestData.freshStream)
  }



}
