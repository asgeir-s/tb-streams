package com.cluda.coinsignals.streams.poststream

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala._
import awscala.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model.{DescribeTableRequest, TableStatus}
import com.cluda.coinsignals.streams.protocoll.NewStream
import com.cluda.coinsignals.streams.util.DatabaseUtil

class PostStreamActor(tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DynamoDB.at(Region.US_WEST_2)

  private val streamsTable: Table = {
    if (dynamoDB.table(tableName).isEmpty) {
      createAndWaitForTable(tableName)
    }
    else {
      dynamoDB.table(tableName).get
    }
  }

  def createAndWaitForTable(tableName: String): Table = {
    log.info("PostStreamActor: creating streamsTable with name " + tableName + " and witing for it to become ACTIVE")
    dynamoDB.createTable(
      name = tableName,
      hashPK = "id" -> AttributeType.String
    )
    val startTime = System.currentTimeMillis()
    val endTime = startTime + (10 * 60 * 1000)
    var tableReady = false
    while (System.currentTimeMillis() < endTime && !tableReady) {
      Thread.sleep(1000 * 10)

      val request = new DescribeTableRequest().withTableName(tableName)
      val table = dynamoDB.describeTable(request).getTable
      if (table == null) {}
      else {
        val tableStatus = table.getTableStatus
        log.info("PostStreamActor: " + tableName + "-tabe  - current state: " + tableStatus)
        if (tableStatus.equals(TableStatus.ACTIVE.toString)) {
          tableReady = true
        }
      }

    }
    log.info("PostStreamActor: streamsTable with name " + tableName + " is ready.")
    dynamoDB.table(tableName).get
  }

  override def receive: Receive = {
    case newStream: NewStream =>
      DatabaseUtil.putNewStream(dynamoDB, streamsTable, newStream)
      sender() ! HttpResponse(StatusCodes.Accepted, entity = """{"id":""" + newStream.id + "}")
  }
}

object PostStreamActor {
  def props(tableName: String): Props = Props(new PostStreamActor(tableName))
}