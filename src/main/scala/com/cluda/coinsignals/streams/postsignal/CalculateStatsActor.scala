package com.cluda.coinsignals.streams.postsignal

import akka.actor.{PoisonPill, Actor, ActorLogging, Props}
import awscala._
import awscala.dynamodbv2.{DynamoDB, Table}
import com.cluda.coinsignals.streams.model.Signal
import com.cluda.coinsignals.streams.protocoll.{DuplicateSignal, StreamDoesNotExistException}
import com.cluda.coinsignals.streams.util.{StreamUtil, DatabaseUtil}

class CalculateStatsActor(streamID: String, tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DynamoDB.at(Region.US_WEST_2)

  if (dynamoDB.table(tableName) isEmpty) {
    log.error("could not find streams-table (" + tableName + ")")
    context.parent ! StreamDoesNotExistException("could not find streams-table (" + tableName + ")")
  }
  private val streamsTable: Table = dynamoDB.table(tableName).get

  val stream = DatabaseUtil.getStream(dynamoDB, streamsTable, streamID)

  if (stream isEmpty) {
    log.error("could not find stream with id " + streamID + " in the streams-table")
    context.parent ! StreamDoesNotExistException("could not find stream with id " + streamID + " in the streams-table")
    self ! PoisonPill
  }

  override def receive: Receive = {
    case signals: Seq[Signal]  =>

      //for safety
      if (stream isEmpty) {
        log.error("could not find stream with id " + streamID + " in the streams-table")
        context.parent ! StreamDoesNotExistException("could not find stream with id " + streamID + " in the streams-table")
        sender() ! StreamDoesNotExistException("could not find stream with id " + streamID + " in the streams-table")
        self ! PoisonPill
      }
      else {
        val newStats = signals.map(signal => {
          val gottenStream = stream.get
          if(gottenStream.status == signal.signal) {
            log.error("same as last signal. Signal id: " + signal.id)
            sender() ! DuplicateSignal("same as last signal")
          }
          else {
            val stats = StreamUtil.updateStreamWitheNewSignal(stream.get, signal)
            DatabaseUtil.putStream(dynamoDB, streamsTable, stats)
            stats
          }
        })
        sender() ! newStats.head
        log.info("stats updated to")
        self ! PoisonPill
      }
  }
}

object CalculateStatsActor {
  def props(streamID: String, tableName: String): Props = Props(new CalculateStatsActor(streamID, tableName: String))
}