package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorFlowMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

class MissingSignalsActor(streamID: String) extends Actor with ActorLogging {

  implicit val materializer = ActorFlowMaterializer()

  implicit val actorSystem = context.system

  implicit val ec = context.system.dispatcher

  val config = ConfigFactory.load()

  val uri = config.getString("microservices.signals")

  override def receive: Receive = {
    case lastProcessedId: Long =>
      val s = sender()
      val requestURI = uri + "/streams/" + streamID + "/signals?fromId=" + lastProcessedId
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = requestURI))

      log.info("MissingSignalsActor for stream " + streamID + " sends request: " + requestURI.toString)

      responseFuture.onComplete {
        case Success(res: HttpResponse) =>
          println("Got back HttpResponse")

        // TODO: handle this respondse when in production

        case Failure(_) =>
          s ! ""
          self ! PoisonPill

      }

  }

}

object MissingSignalsActor {
  def props(streamID: String): Props = Props(new MissingSignalsActor(streamID))
}