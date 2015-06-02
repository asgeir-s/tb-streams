package com.cluda.coinsignals.streams.getstream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala._
import com.cluda.coinsignals.streams.util.DatabaseUtil
import com.typesafe.config.ConfigFactory

class GetStreamActor(tableName: String) extends Actor with ActorLogging {
  val config = ConfigFactory.load()
  implicit val region: Region = awscala.Region.US_WEST_2
  val awscalaCredentials = BasicCredentialsProvider(
    config.getString("aws.accessKeyId"),
    config.getString("aws.secretAccessKey"))


  implicit val dynamoDB = awscala.dynamodbv2.DynamoDB(awscalaCredentials)

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