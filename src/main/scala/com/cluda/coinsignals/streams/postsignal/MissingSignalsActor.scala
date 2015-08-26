package com.cluda.coinsignals.streams.postsignal

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.cluda.coinsignals.protocol.Sec
import com.cluda.coinsignals.streams.model.Signal
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Future, Promise}

class MissingSignalsActor(streamID: String) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val actorSystem = context.system
  implicit val ec = context.system.dispatcher

  val config = ConfigFactory.load()
  val signalsHost = config.getString("microservices.signals")
  val signalsPort = config.getInt("microservices.signalsPort")

  def getSignalsFromId(id: Long): Future[Seq[Signal]] = {
    val promise = Promise[Seq[Signal]]()
    val theFuture = promise.future
    import spray.json._
    import com.cluda.coinsignals.streams.model.SignalJsonProtocol._
    import com.cluda.coinsignals.protocol.Sec._

    val conn = Http().outgoingConnection(signalsHost, port = signalsPort)
    val path = "/streams/" + streamID + "/signals?fromId=" + id
    val request = secureHttpRequest(GET, uri = path)
    log.info("MissingSignalsActor for stream " + streamID + " sends request: " + request.toString)
    Source.single(request).via(conn).runWith(Sink.head[HttpResponse]).map { x =>
      Unmarshal(x.entity).to[String].map { body =>
        val validatedAndDecrypted = Sec.validateAndDecryptMessage(body)
        if(validatedAndDecrypted.isDefined) {
          promise.success(validatedAndDecrypted.get.parseJson.convertTo[Seq[Signal]])
        }
        else {
          log.error("Could not validate and decrypt message. The raw http body was: " + body)
          promise.failure(new Exception("Could not validate and decrypt message. The raw http body was: " + body))

        }
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