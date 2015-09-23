package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import awscala.dynamodbv2.Table
import com.cluda.coinsignals.streams.model.{SStream, Signal}
import com.cluda.coinsignals.streams.protocoll.{FatalStreamCorruptedException, StreamDoesNotExistException, UnexpectedSignalException}
import com.cluda.coinsignals.streams.util.{DatabaseUtil, StreamUtil}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class CalculateStatsActor(streamID: String, tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec = context.dispatcher

  if (dynamoDB.table(tableName).isEmpty) {
    log.error("CalculateStatsActor(" + streamID + "): could not find streams-table (" + tableName + ")")
    context.parent ! StreamDoesNotExistException(
      "could not find streams-table (" + tableName + ")")
    self ! PoisonPill
  }

  private val streamsTable: Table = dynamoDB.table(tableName).get
  val stream = Await.result(DatabaseUtil.getStream(streamsTable, streamID), 3 seconds)

  override def receive: Receive = {
    case signals: Seq[Signal] =>
      val s = sender()

      log.info("CalculateStatsActor(" + streamID + "): Received " + signals.length + " signal(s)")
      log.info("signal: " + signals.head.signal)

      //for safety
      if (stream.isEmpty) {
        log.error("CalculateStatsActor(" + streamID + "): could not find stream with id " + streamID + " in the streams-table")
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
          log.warning("CalculateStatsActor(" + streamID + "): only received signal(s) with ID(s) that has " +
            "already been processed. Returns 'UnexpectedSignalException'.")

          sender() ! UnexpectedSignalException(
            "only received signal(s) with ID(s) that has already been processed.")

          self ! PoisonPill
        }
        else if (!newSignals.exists(_.id == sStream.idOfLastSignal + 1) &&
          sStream.stats.numberOfSignals > 0) {

          log.warning("CalculateStatsActor(" + streamID + "): received signal(s) with ID(s) that does " +
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
              log.error("CalculateStatsActor(" + streamID + "): [FatalStreamCorruptedException] This " +
                "signal has the same (position-)signal as the last signal. Signal id: " +
                signal.id)

              sender() ! FatalStreamCorruptedException(streamID,
                "this signal has the same (position-)signal as the last signal")

            }
            else if (sStream.status == 1 && signal.signal == -1 ||
              sStream.status == -1 && signal.signal == 1) {

              log.error("CalculateStatsActor(" + streamID + "): [FatalStreamCorruptedException] invalid " +
                "sequence of signals (going from LONG to SHORT or SHORT to LONG without " +
                "closing first).")

              sender() ! FatalStreamCorruptedException(streamID,
                "invalid sequence of signals (going from LONG to SHORT or SHORT to LONG " +
                  "without closing first).")

              self ! PoisonPill
            }
            else {
              log.info("CalculateStatsActor(" + streamID + "): signal with id " + signal.id + " was accepted as new signal. Starting to update the stream stats.")
              sStream = StreamUtil.updateStreamWitheNewSignal(sStream, signal)
            }
          })


          DatabaseUtil.updateStream(streamsTable, sStream).map { returnedStream =>
            s ! returnedStream
            log.info("CalculateStatsActor(" + returnedStream.id.get + "): stream updated in database. New stream object: " +
              sStream)
          }.recover {
            case e: Throwable =>
              log.error("CalculateStatsActor(" + sStream.id.get + "): failed. Error: " + e.toString)
              s ! e
          }.andThen {
            case _ =>
              self ! PoisonPill
          }

        }
      }
  }
}

object CalculateStatsActor {
  def props(streamID: String, tableName: String): Props =
    Props(new CalculateStatsActor(streamID, tableName: String))
}