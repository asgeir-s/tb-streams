package com.cluda.tradersbit.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.cluda.tradersbit.streams.model.{Signal, SignalJsonProtocol}
import com.cluda.tradersbit.streams.util.HttpUtil
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, Promise}

class MissingSignalsActor(globalRequestID: String, streamID: String) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val actorSystem = context.system
  implicit val ec = context.system.dispatcher

  val config = ConfigFactory.load()
  val signalsHost = config.getString("microservices.signals")
  val signalsPort = config.getInt("microservices.signalsPort")
  private val https = config.getBoolean("microservices.https")
  private val authorizationHeader = RawHeader("Authorization", "apikey " + config.getString("microservices.signalsApiKey"))


  def getSignalsFromId(id: Long): Future[Seq[Signal]] = {
    import SignalJsonProtocol._
    import spray.json._

    val promise = Promise[Seq[Signal]]()

    val path = "/streams/" + streamID + "/signals?fromId=" + id
    log.info(s"[$globalRequestID]: (StreamID: $streamID): Sends request. Path: " + path)

    HttpUtil.request(
      HttpMethods.GET,
      https,
      signalsHost,
      path,
      headers = List(RawHeader("Global-Request-ID", globalRequestID), authorizationHeader)
    ).map { x =>
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