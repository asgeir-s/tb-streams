package com.cluda.tradersbit.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.cluda.tradersbit.streams.model.{Signal, SignalJsonProtocol}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, Promise}

class MissingSignalsActor(globalRequestID: String, streamID: String) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val actorSystem = context.system
  implicit val ec = context.system.dispatcher

  val config = ConfigFactory.load()
  val signalsHost = config.getString("microservices.signals")
  val signalsPort = config.getInt("microservices.signalsPort")

  def getSignalsFromId(id: Long): Future[Seq[Signal]] = {
    val promise = Promise[Seq[Signal]]()

    import SignalJsonProtocol._
    import spray.json._

    val conn = Http().outgoingConnection(signalsHost, port = signalsPort)
    val path = "/streams/" + streamID + "/signals?fromId=" + id
    val request = HttpRequest(GET, uri = path)
    log.info(s"[$globalRequestID]: (StreamID: $streamID): Sends request: " + request.toString)
    Source.single(request).via(conn).runWith(Sink.head[HttpResponse]).map { x =>
      Unmarshal(x.entity).to[String].map { body =>
          promise.success(body.parseJson.convertTo[Seq[Signal]])
      }
    }.recover{
      case e: Throwable =>
        log.error(s"[$globalRequestID]: (StreamID: $streamID): Could not get signals from $signalsHost$path. Error: " + e.toString)
        promise.failure(e)
    }
    promise.future
  }

  override def receive: Receive = {
    case lastProcessedId: Long =>
      val s = sender()

      getSignalsFromId(lastProcessedId).map { signals =>
        log.info(s"[$globalRequestID]: (StreamID: $streamID): Got back " + signals.length + " signals")
        s ! signals
      }.recover{
        case e: Throwable =>
          log.error(s"[$globalRequestID]: (StreamID: $streamID): Failed getting signals from id: $lastProcessedId. Error: " + e.toString)
          s ! e
      }.andThen{
        case _ => self ! PoisonPill
      }

  }

}

object MissingSignalsActor {
  def props(globalRequestID: String, streamID: String): Props = Props(new MissingSignalsActor(globalRequestID, streamID))
}