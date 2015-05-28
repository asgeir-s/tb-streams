package com.cluda.coinsignals.streams.getstream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala._
import awscala.dynamodbv2.DynamoDB
import com.cluda.coinsignals.streams.util.DatabaseUtil

class GetStreamActor(tableName: String) extends Actor with ActorLogging {

  implicit val dynamoDB = DynamoDB.at(Region.US_WEST_2)

  override def receive: Receive = {
    case (streamId: String, privateInfo: Boolean) =>

      if (dynamoDB.table(tableName).isEmpty) {
        sender() ! HttpResponse(StatusCodes.NotFound)
      }
      else {
        val table = dynamoDB.table(tableName).get
        val stream = DatabaseUtil.getStream(dynamoDB, table, streamId)
        if(stream.isDefined) {
          if (privateInfo) {
            sender() ! HttpResponse(StatusCodes.OK, entity = stream.get.privateJson)
          }
          else {
            sender() ! HttpResponse(StatusCodes.OK, entity = stream.get.publicJsonWithStatus)
          }
        }
        else {
          sender() ! HttpResponse(StatusCodes.NotFound)
        }
        self ! PoisonPill
      }
  }

}

object GetStreamActor {
  def props(tableName: String): Props = Props(new GetStreamActor(tableName))
}