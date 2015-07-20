package com.cluda.coinsignals.streams.getstream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala._
import com.cluda.coinsignals.protocol.Sec._
import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class GetStreamsActor(tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())

  val tableIsEmpty = dynamoDB.table(tableName).isEmpty

  override def receive: Receive = {
    case (streamId: String, privateInfo: Boolean) =>
      if (tableIsEmpty) {
        sender() ! secureHttpResponse(StatusCodes.NotFound)
      }
      else {
        val table = dynamoDB.table(tableName).get
        val stream = DatabaseUtil.getStream(dynamoDB, table, streamId)
        if (stream.isDefined) {
          if (privateInfo) {
            sender() ! secureHttpResponse(StatusCodes.OK, entity = stream.get.privateJson)
          }
          else {
            sender() ! secureHttpResponse(StatusCodes.OK, entity = stream.get.publicJson)
          }
        }
        else {
          sender() ! secureHttpResponse(StatusCodes.NotFound)
        }
        self ! PoisonPill
      }

    case "all" =>
      if (tableIsEmpty) {
        sender() ! secureHttpResponse(StatusCodes.OK, entity = "[]")
      }
      else {
        val table = dynamoDB.table(tableName).get
        sender() ! secureHttpResponse(StatusCodes.OK, entity =
          "[" + DatabaseUtil.getAllStreams(dynamoDB, table).map(_.publicJson).mkString(",") + "]")
      }

  }

}

object GetStreamsActor {
  def props(tableName: String): Props = Props(new GetStreamsActor(tableName))
}