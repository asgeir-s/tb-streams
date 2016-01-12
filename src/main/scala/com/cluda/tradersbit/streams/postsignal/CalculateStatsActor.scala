package com.cluda.tradersbit.streams.postsignal

import akka.actor._
import awscala.dynamodbv2.Table
import com.cluda.tradersbit.streams.model.Signal
import com.cluda.tradersbit.streams.protocoll.FatalStreamCorruptedException
import com.cluda.tradersbit.streams.util.StreamUtil
import com.cluda.tradersbit.streams.model.{SStream, Signal}
import com.cluda.tradersbit.streams.protocoll.{FatalStreamCorruptedException, StreamDoesNotExistException, UnexpectedSignalException}
import com.cluda.tradersbit.streams.util.{StreamUtil, DatabaseUtil}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class CalculateStatsActor(globalRequestID: String, streamID: String, tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec = context.dispatcher

  var postSignalActorRef: Option[ActorRef] = None

  if (dynamoDB.table(tableName).isEmpty) {
    log.error(s"[$globalRequestID]: (StreamID: $streamID): Could not find streams-table (" + tableName + ")")
    context.parent ! (globalRequestID, StreamDoesNotExistException(
      s"[$globalRequestID]: (StreamID: $streamID): Could not find streams-table (" + tableName + ")"))
    self ! PoisonPill
  }

  private val streamsTable: Table = dynamoDB.table(tableName).get
  val streamInList = Await.result(DatabaseUtil.getStreams(streamsTable, List(streamID)), 3 seconds)

  override def receive: Receive = {
    case signals: Seq[Signal] =>
      val s = sender()
      postSignalActorRef = Some(s)

      log.info(s"[$globalRequestID]: (StreamID: $streamID): Received " + signals.length + " signal(s).")

      //for safety
      if (streamInList.isEmpty) {
        log.error(s"[$globalRequestID]: (StreamID: $streamID): Could not find stream with id " + streamID + " in the streams-table")
        context.parent ! StreamDoesNotExistException(
          s"[$globalRequestID]: (StreamID: $streamID): Could not find stream with id " + streamID + " in the streams-table")

        sender() ! StreamDoesNotExistException(
          s"[$globalRequestID]: (StreamID: $streamID): Could not find stream with id " + streamID + " in the streams-table")

        self ! PoisonPill
      }

      else {
        var sStream: SStream = streamInList.get.last
        val newSignals = signals.filter(_.id > sStream.idOfLastSignal)

        if (newSignals.isEmpty) {
          log.warning(s"[$globalRequestID]: (StreamID: $streamID): Only received signal(s) with ID(s) that has " +
            "already been processed. Returns 'UnexpectedSignalException'.")

          sender() ! UnexpectedSignalException(
            s"[$globalRequestID]: (StreamID: $streamID): Only received signal(s) with ID(s) that has already been processed.")

          self ! PoisonPill
        }
        else if (!newSignals.exists(_.id == sStream.idOfLastSignal + 1) &&
          newSignals.exists(_.id > sStream.idOfLastSignal)) {
          log.warning(s"[$globalRequestID]: (StreamID: $streamID): Received signal(s) with ID(s) that does " +
            "not include the expected next ID. The ID(s) are (all) higher then the " +
            "next expected next signal's id. Starts MissingSignalsActor to retrieve " +
            "the missing signals.")

          context.actorOf(MissingSignalsActor.props(globalRequestID, streamID)) ! sStream.idOfLastSignal
        }
        else {
          newSignals.sortBy(_.id).foreach(signal => {
            if (signal.id != sStream.idOfLastSignal + 1 && sStream.stats.numberOfSignals > 0) {

              sender() ! FatalStreamCorruptedException(streamID,
                s"[$globalRequestID]: (StreamID: $streamID): Try to add a signal that did no have the last id + 1")
            }
            else if (sStream.status == signal.signal) {
              log.error(s"[$globalRequestID]: (StreamID: $streamID) [FatalStreamCorruptedException]: This " +
                "signal has the same (position-)signal as the last signal. Signal id: " +
                signal.id)

              sender() ! FatalStreamCorruptedException(streamID,
                s"[$globalRequestID]: (StreamID: $streamID): this signal has the same (position-)signal as the last signal")

            }
            else if (sStream.status == 1 && signal.signal == -1 ||
              sStream.status == -1 && signal.signal == 1) {

              log.error(s"[$globalRequestID]: (StreamID: $streamID): [FatalStreamCorruptedException] invalid " +
                "sequence of signals (going from LONG to SHORT or SHORT to LONG without " +
                "closing first).")

              sender() ! FatalStreamCorruptedException(streamID,
                s"[$globalRequestID]: (StreamID: $streamID): Invalid sequence of signals (going from LONG to SHORT or SHORT to LONG " +
                  "without closing first).")

              self ! PoisonPill
            }
            else {
              log.info(s"[$globalRequestID]: (StreamID: $streamID): Signal with id " + signal.id + " was accepted as new signal. Starting to update the stream stats.")
              sStream = StreamUtil.updateStreamWitheNewSignal(sStream, signal)
            }
          })


          DatabaseUtil.updateStream(streamsTable, sStream, signals.maxBy(_.id)).map { returnedStream =>
            s ! returnedStream
            log.info(s"[$globalRequestID]: (StreamID: " + returnedStream.id.get + "): Stream updated in database. New stream object: " +
              sStream)
          }.recover {
            case e: Throwable =>
              log.error(s"[$globalRequestID]: (StreamID: " + sStream.id.get + "): failed. Error: " + e.toString)
              s ! e
          }.andThen {
            case _ =>
              self ! PoisonPill
          }

        }
      }

    case e: Throwable =>
      log.error(s"[$globalRequestID]: (StreamID: $streamID): Got error from 'MissingSignalsActor'. Returning error to 'PostSignalActor'. Error: " + e.toString)
      postSignalActorRef.get ! e
  }
}

object CalculateStatsActor {
  def props(globalRequestID: String, streamID: String, tableName: String): Props =
    Props(new CalculateStatsActor(globalRequestID, streamID, tableName: String))
}