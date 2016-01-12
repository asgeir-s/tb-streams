package com.cluda.admin

import awscala.dynamodbv2.DynamoDB
import com.cluda.tradersbit.streams.model.{ComputeComponents, StreamStats, SStream}
import com.cluda.tradersbit.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

/**
  * Created by sogasg on 12/01/16.
  */
object ResetStreams {

  val tableName = "streams"
  val tableRegion = "us-east-1"

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load(),Some(tableRegion))

  def main(args: Array[String]): Unit = {
    val table = dynamoDB.table(tableName).get
    val allStreams: Seq[SStream] = DatabaseUtil.getAllStreams(table)

    //backup

    // reset streams
    val resatStreams: Seq[SStream] = allStreams.map((stream: SStream) => {
      stream.copy(
        idOfLastSignal=0,
        status=0,
        lastSignal = None,
        stats = new StreamStats,
        computeComponents = new ComputeComponents)
    })


    // write streams back
    resatStreams.map((stream: SStream) => {
      DatabaseUtil.tableForcePut(table, stream, None)
    })
  }
}
