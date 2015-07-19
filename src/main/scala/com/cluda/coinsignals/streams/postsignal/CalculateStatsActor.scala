package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import awscala._
import awscala.dynamodbv2.{DynamoDB, Table}
import com.amazonaws.regions.Region
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.protocoll.{FatalStreamCorruptedException, StreamDoesNotExistException, UnexpectedSignalException}
import com.cluda.coinsignals.streams.util.{DatabaseUtil, StreamUtil}
import com.typesafe.config.ConfigFactory

class CalculateStatsActor(streamID: String, tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())


  if (dynamoDB.table(tableName).isEmpty) {
    log.error("could not find streams-table (" + tableName + ")")
    context.parent ! StreamDoesNotExistException(
      "could not find streams-table (" + tableName + ")")
    self ! PoisonPill
  }

  private val streamsTable: Table = dynamoDB.table(tableName).get
  val stream = DatabaseUtil.getStream(dynamoDB, streamsTable, streamID)

  if (stream.isEmpty) {
    log.error("could not find stream with id " + streamID + " in the streams-table")
    context.parent ! StreamDoesNotExistException(
      "could not find stream with id " + streamID + " in the streams-table")

    self ! PoisonPill
  }

  override def receive: Receive = {
    case signals: Seq[Signal] =>

      //for safety
      if (stream.isEmpty) {
        log.error("could not find stream with id " + streamID + " in the streams-table")
        context.parent ! StreamDoesNotExistException(
          "could not find stream with id " + streamID + " in the streams-table")

        sender() ! StreamDoesNotExistException(
          "could not find stream with id " + streamID + " in the streams-table")

        self ! PoisonPill
      }

      else {
        var sStream: SStream = stream.get
        val newSignals = signals.filter(_.id > sStream.idOfLastSignal)

        if (newSignals.isEmpty) {
          log.warning("CalculateStatsActor: only received signal(s) with ID(s) that has " +
            "already been processed. Returns 'UnexpectedSignalException'.")

          sender() ! UnexpectedSignalException(
            "only received signal(s) with ID(s) that has already been processed.")

          self ! PoisonPill
        }
        else if (!newSignals.exists(_.id == sStream.idOfLastSignal + 1) &&
          sStream.stats.numberOfSignals > 0) {

          log.warning("CalculateStatsActor: received signal(s) with ID(s) that does " +
            "not include the expected next ID. The ID(s) are (all) higher then the " +
            "next expected next signal's id. Starts MissingSignalsActor to retrieve " +
            "the missing signals.")

          context.actorOf(MissingSignalsActor.props(streamID)) ! sStream.idOfLastSignal
        }
        else {
          newSignals.sortBy(_.id).foreach(signal => {
            if (signal.id != sStream.idOfLastSignal + 1 && sStream.stats.numberOfSignals > 0) {

              sender() ! FatalStreamCorruptedException(streamID,
                "try to add a signal that did no have the last id + 1")
            }
            else if (sStream.status == signal.signal) {
              log.error("CalculateStatsActor: [FatalStreamCorruptedException] This " +
                "signal has the same (position-)signal as the last signal. Signal id: " +
                signal.id)

              sender() ! FatalStreamCorruptedException(streamID,
                "this signal has the same (position-)signal as the last signal")

            }
            else if (sStream.status == 1 && signal.signal == -1 ||
              sStream.status == -1 && signal.signal == 1) {

              log.error("CalculateStatsActor: [FatalStreamCorruptedException] invalid " +
                "sequence of signals (going from LONG to SHORT or SHORT to LONG without " +
                "closing first).")

              sender() ! FatalStreamCorruptedException(streamID,
                "invalid sequence of signals (going from LONG to SHORT or SHORT to LONG " +
                  "without closing first).")

              self ! PoisonPill
            }
            else {
              sStream = StreamUtil.updateStreamWitheNewSignal(sStream, signal)
            }
          })
          DatabaseUtil.putStream(dynamoDB, streamsTable, sStream)
          sender() ! sStream
          log.info("CalculateStatsActor: stream updated in database. New stream object: " +
            sStream)

          self ! PoisonPill
        }
      }
  }
}

object CalculateStatsActor {
  def props(streamID: String, tableName: String): Props =
    Props(new CalculateStatsActor(streamID, tableName: String))
}