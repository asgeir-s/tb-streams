package com.cluda.tradersbit.streams.getstream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.cluda.tradersbit.streams.model.SStream
import com.cluda.tradersbit.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class GetStreamsActor(globalRequestID: String, tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec = context.dispatcher

  val tableIsEmpty = dynamoDB.table(tableName).isEmpty

  override def receive: Receive = {
    case (streams: List[String], privateInfo: Boolean) =>
      val s = sender()
      if (tableIsEmpty) {
        log.info(s"[$globalRequestID]: Their are no streams. Returning HttpResponds with empty array.")
        sender() ! HttpResponse(StatusCodes.OK, entity = "[]")
      }
      else {
        val table = dynamoDB.table(tableName).get
        DatabaseUtil.getStreams(table, streams).map {
          case Some(sStreams: List[SStream]) =>
            if (privateInfo) {
              log.info(s"[$globalRequestID]: Returning HttpResponds with stream (including private info).")
              s ! HttpResponse(StatusCodes.OK, entity = sStreams.map((stream: SStream) => {stream.privateJson}).mkString(","))
            }
            else {
              log.info(s"[$globalRequestID]: Returning HttpResponds with stream (only public info).")
              s ! HttpResponse(StatusCodes.OK, entity = sStreams.map((stream: SStream) => {stream.publicJson}).mkString(","))
            }
          case None =>
            log.info(s"[$globalRequestID]: Could not find streams with id's:" + streams.mkString(",") + ". Returning HttpResponds-NotFound.")
            s ! HttpResponse(StatusCodes.NotFound)

        }.recover {
          case e: Throwable =>
            log.error(s"[$globalRequestID]: Error running 'DatabaseUtil.getStreams' for streams: " + streams.mkString(",") + ". Error: " + e.toString)
            s ! HttpResponse(StatusCodes.InternalServerError)
        }.andThen {
          case _ => self ! PoisonPill
        }
      }

    case "all" =>
      val s = sender()
      if (tableIsEmpty) {
        s ! HttpResponse(StatusCodes.OK, entity = "[]")
        log.info(s"[$globalRequestID]: Their are no streams. Returning HttpResponds with empty array.")
      }
      else {
        val table = dynamoDB.table(tableName).get
        DatabaseUtil.getAllStreams(table).map {
          case streams: Seq[SStream] =>
            log.info(s"[$globalRequestID]: Returning HttpResponds with array of all streams. Length: " + streams.length)
            s ! HttpResponse(StatusCodes.OK, entity =
              "[" + streams.map(_.publicJson).mkString(",") + "]")
        }.recover {
          case e: Throwable =>
            log.error(s"[$globalRequestID]: Error running 'DatabaseUtil.getAllStreams'. Error: " + e.toString)
            s ! HttpResponse(StatusCodes.InternalServerError)
        }.andThen {
          case _ => self ! PoisonPill
        }
      }

  }

}

object GetStreamsActor {
  def props(globalRequestID: String, tableName: String): Props = Props(new GetStreamsActor(globalRequestID, tableName))
}