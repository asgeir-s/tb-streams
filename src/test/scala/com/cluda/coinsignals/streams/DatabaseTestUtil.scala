package com.cluda.coinsignals.streams

import awscala.dynamodbv2.{AttributeType, DynamoDB, Table}
import com.amazonaws.services.dynamodbv2.model.{DescribeTableRequest, TableStatus}
import com.cluda.coinsignals.streams.model.SStream
import com.cluda.coinsignals.streams.protocoll.NewStream

/**
 * Created by sogasg on 21/05/15.
 */
object DatabaseTestUtil {

  def createStreamsTable(implicit dynamoDB: DynamoDB, tableName: String): Table = {
    if (dynamoDB.table(tableName) isEmpty) {
      createAndWaitForTable(dynamoDB, tableName)
    }
    else {
      dynamoDB.table(tableName).get
    }
  }

  def createAndWaitForTable(implicit dynamoDB: DynamoDB, tableName: String): Table = {
    println("creats test table with name: " + tableName + " and waits for it to be available.")
    dynamoDB.createTable(
      name = tableName,
      hashPK = "id" -> AttributeType.String
    )
    val startTime = System.currentTimeMillis()
    val endTime = startTime + (10 * 60 * 1000)
    var tableReady = false
    while (System.currentTimeMillis() < endTime && !tableReady) {
      Thread.sleep(1000 * 10)
      print(".")
      val request = new DescribeTableRequest().withTableName(tableName)
      val table = dynamoDB.describeTable(request).getTable()
      if (table == null) {}
      else {
        val tableStatus = table.getTableStatus()
        if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
          tableReady = true
        }
      }

    }
    println("table with name: " + tableName + " is ready.")
    dynamoDB.table(tableName).get
  }
}
