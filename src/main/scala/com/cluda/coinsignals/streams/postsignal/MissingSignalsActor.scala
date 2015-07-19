package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.cluda.coinsignals.streams.model.Signal
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, Promise}

class MissingSignalsActor(streamID: String) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val actorSystem = context.system
  implicit val ec = context.system.dispatcher

  val config = ConfigFactory.load()
  val host = config.getString("microservices.signals")

  def getSignalsFromId(id: Long): Future[Seq[Signal]] = {
    val promise = Promise[Seq[Signal]]()
    val theFuture = promise.future
    import spray.json._
    import com.cluda.coinsignals.streams.model.SignalJsonProtocol._

    val conn = Http().outgoingConnection(host)
    val path = "/streams/" + streamID + "/signals?fromId=" + id
    val request = HttpRequest(GET, uri = path)
    log.info("MissingSignalsActor for stream " + streamID + " sends request: " + request.toString)
    Source.single(request).via(conn).runWith(Sink.head[HttpResponse]).map { x =>
      Unmarshal(x.entity).to[String].map { body =>
        promise.success(body.parseJson.convertTo[Seq[Signal]])
      }
    }
    theFuture
  }

  override def receive: Receive = {
    case lastProcessedId: Long =>
      val s = sender()

      getSignalsFromId(lastProcessedId).map { signals =>
        log.info("MissingSignalsActor for stream " + streamID + " got back " + signals.length + " signals")
        s ! signals
        self ! PoisonPill
      }


  }

}

object MissingSignalsActor {
  def props(streamID: String): Props = Props(new MissingSignalsActor(streamID))
}