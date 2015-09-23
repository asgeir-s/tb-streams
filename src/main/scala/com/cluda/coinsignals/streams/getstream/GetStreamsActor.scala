package com.cluda.coinsignals.streams.getstream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.cluda.coinsignals.streams.model.SStream
import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class GetStreamsActor(tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec = context.dispatcher

  val tableIsEmpty = dynamoDB.table(tableName).isEmpty

  override def receive: Receive = {
    case (streamId: String, privateInfo: Boolean) =>
      val s = sender()
      if (tableIsEmpty) {
        sender() ! HttpResponse(StatusCodes.OK, entity = "[]")
      }
      else {
        val table = dynamoDB.table(tableName).get
        DatabaseUtil.getStream(table, streamId).map {
          case Some(sStream: SStream) =>
            if (privateInfo) {
              s ! HttpResponse(StatusCodes.OK, entity = sStream.privateJson)
            }
            else {
              s ! HttpResponse(StatusCodes.OK, entity = sStream.publicJson)
            }
          case None =>
            s ! HttpResponse(StatusCodes.NotFound)

        }.recover {
          case e: Throwable =>
            log.error("Error running 'DatabaseUtil.getStream' for stream: " + streamId + ". Error: " + e.toString)
            s ! HttpResponse(StatusCodes.InternalServerError)
        }.andThen {
          case _ => self ! PoisonPill
        }
      }

    case "all" =>
      val s = sender()
      if (tableIsEmpty) {
        s ! HttpResponse(StatusCodes.OK, entity = "[]")
      }
      else {
        val table = dynamoDB.table(tableName).get
        DatabaseUtil.getAllStreams(table).map {
          case streams: Seq[SStream] =>
            s ! HttpResponse(StatusCodes.OK, entity =
              "[" + streams.map(_.publicJson).mkString(",") + "]")
        }.recover {
          case e: Throwable =>
            log.error("Error running 'DatabaseUtil.getAllStreams'. Error: " + e.toString)
            s ! HttpResponse(StatusCodes.InternalServerError)
        }.andThen {
          case _ => self ! PoisonPill
        }
      }

  }

}

object GetStreamsActor {
  def props(tableName: String): Props = Props(new GetStreamsActor(tableName))
}