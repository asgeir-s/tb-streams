package com.cluda.admin

import awscala.dynamodbv2.DynamoDB
import com.cluda.tradersbit.streams.model.{ComputeComponents, StreamStats, SStream}
import com.cluda.tradersbit.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

/**
  * Created by sogasg on 12/01/16.
  */
object ResetStream {

  val tableName = "streams"
  val tableRegion = "us-east-1"
  val streamID = "4367c697-4da5-4b1d-a09e-302fa69a4e10"//"4367c697-4da5-4b1d-a09e-302fa69a4e10"
  val name = "CashCow"

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load(),Some(tableRegion))

  def main(args: Array[String]): Unit = {
    val table = dynamoDB.table(tableName).get
    val allStreams: Seq[SStream] = DatabaseUtil.getAllStreams(table)

    val streamToReset = allStreams.filter((stream) => stream.id.get == streamID)

    assert(streamToReset.length == 1)
    assert(streamToReset.head.id.get == streamID)
    assert(streamToReset.head.name == name)

    //backup

    // reset streams
    val resatStream: Seq[SStream] = streamToReset.map((stream: SStream) => {
      stream.copy(
        idOfLastSignal=0,
        status=0,
        lastSignal = None,
        stats = new StreamStats,
        computeComponents = new ComputeComponents)
    })

    assert(resatStream.length == 1)
    assert(resatStream.head.id.get == streamID)
    assert(resatStream.head.name == name)


    // write streams back
    resatStream.map((stream: SStream) => {
      DatabaseUtil.tableForcePut(table, stream, None)
    })
  }
}
