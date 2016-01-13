package com.cluda.tradersbit.streams.getapikeyid

import akka.actor.{PoisonPill, Props, ActorLogging, Actor}
import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import com.cluda.tradersbit.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

/**
  * Created by sogasg on 13/01/16.
  */
class GetApiKeyIdActor(globalRequestID: String, tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec = context.dispatcher
  val tableOpt = dynamoDB.table(tableName)

  override def receive: Receive = {
    case NewApiKeyId(streamId: String) =>
      val s = sender()
      tableOpt match {
        case Some(table: awscala.dynamodbv2.Table) =>
          DatabaseUtil.generateAndSetApiKeyId(table, streamId).map { apiKeyId =>
            s ! HttpResponse(StatusCodes.OK, entity = apiKeyId)
            log.info(s"[$globalRequestID]: api key generated, saved and returned")
            self ! PoisonPill
          }
        case None =>
          s ! HttpResponse(StatusCodes.NotFound)
          log.warning(s"[$globalRequestID]: their is now table with this name: $tableName")
          self ! PoisonPill
      }

  }
}

object GetApiKeyIdActor {
  def props(globalRequestID: String, tableName: String): Props = Props(new GetApiKeyIdActor(globalRequestID, tableName))
}

case class NewApiKeyId(streamId: String)