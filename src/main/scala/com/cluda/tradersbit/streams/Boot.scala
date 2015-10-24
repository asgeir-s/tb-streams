package com.cluda.tradersbit.streams

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


object Boot extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  override val timeout = Timeout(2.minutes)

  override val streamsTableName: String = config.getString("aws.dynamo.streamsTable")

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}