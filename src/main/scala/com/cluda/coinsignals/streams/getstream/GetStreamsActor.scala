package com.cluda.coinsignals.streams.getstream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala._
import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class GetStreamsActor(tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())

  val tableIsEmpty = dynamoDB.table(tableName).isEmpty

  override def receive: Receive = {
    case (streamId: String, privateInfo: Boolean) =>
      if (tableIsEmpty) {
        sender() ! HttpResponse(StatusCodes.NotFound)
      }
      else {
        val table = dynamoDB.table(tableName).get
        val stream = DatabaseUtil.getStream(dynamoDB, table, streamId)
        if (stream.isDefined) {
          if (privateInfo) {
            sender() ! HttpResponse(StatusCodes.OK, entity = stream.get.privateJson)
          }
          else {
            sender() ! HttpResponse(StatusCodes.OK, entity = stream.get.publicJson)
          }
        }
        else {
          sender() ! HttpResponse(StatusCodes.NotFound)
        }
        self ! PoisonPill
      }

    case "all" =>
      if (tableIsEmpty) {
        sender() ! HttpResponse(StatusCodes.OK, entity = "[]")
      }
      else {
        val table = dynamoDB.table(tableName).get
        sender() ! HttpResponse(StatusCodes.OK, entity =
          "[" + DatabaseUtil.getAllStreams(dynamoDB, table).map(_.publicJson).mkString(",") + "]")
      }

  }

}

object GetStreamsActor {
  def props(tableName: String): Props = Props(new GetStreamsActor(tableName))
}