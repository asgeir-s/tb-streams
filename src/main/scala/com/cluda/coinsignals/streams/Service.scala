package com.cluda.coinsignals.streams

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.FlowMaterializer
import akka.util.Timeout
import com.cluda.coinsignals.streams.getstream.GetStreamActor
import com.cluda.coinsignals.streams.model.{Signal, SignalJsonProtocol}
import com.cluda.coinsignals.streams.postsignal.PostSignalActor
import com.cluda.coinsignals.streams.poststream.PostStreamActor
import com.cluda.coinsignals.streams.protocoll.{NewStream, NewStreamJsonProtocol}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}

trait Service {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: FlowMaterializer

  implicit val timeout: Timeout

  def config: Config

  val logger: LoggingAdapter

  val streamsTableName: String

  /**
   * Start a actor and pass it the decodedHttpRequest.
   * Returns a future. If anything fails it returns HttpResponse with "BadRequest",
   * else it returns the HttpResponse returned by the started actor
   *
   * @param props of the actor to start
   * @return Future[HttpResponse]
   */
  def perRequestActor[T](props: Props, message: T): Future[HttpResponse] = {
    (system.actorOf(props) ? message)
      .recover { case _ => HttpResponse(BadRequest, entity = "BadRequest") }
      .asInstanceOf[Future[HttpResponse]]
  }

  val routes = {
    logRequestResult("signals") {
      pathPrefix("streams") {
        pathPrefix(Segment) { streamID =>
          get {
            logRequestResult("GET streams/" + streamID)
            parameters('private.as[Boolean].?) { privateInfo =>
              complete {
                perRequestActor[(String, Boolean)](GetStreamActor.props(streamsTableName), (streamID, privateInfo.getOrElse(false)))
              }
            }
          } ~
            pathPrefix("signals") {
              post {
                logRequestResult("POST streams/" + streamID + "/signals")
                entity(as[String]) { signalString =>
                  import SignalJsonProtocol._
                  import spray.json._
                  val signals = signalString.parseJson.convertTo[Seq[Signal]]
                  complete {
                    perRequestActor[Seq[Signal]](PostSignalActor.props(streamID, streamsTableName), signals)
                  }
                }
              }
            }
        } ~
          post {
            logRequestResult("POST streams")
            import NewStreamJsonProtocol._
            import spray.json._
            entity(as[String]) { streamString =>
              val newStream: NewStream = streamString.parseJson.convertTo[NewStream]
              complete {
                perRequestActor[NewStream](PostStreamActor.props(streamsTableName), newStream)
              }
            }
          }
      }

    }
  }
}
