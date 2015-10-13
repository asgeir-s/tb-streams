package com.cluda.coinsignals.streams

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpMethods, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.cluda.coinsignals.streams.getstream.GetStreamsActor
import com.cluda.coinsignals.streams.model.{Signal, SignalJsonProtocol}
import com.cluda.coinsignals.streams.postsignal.PostSignalActor
import com.cluda.coinsignals.streams.poststream.{ChangeSubscriptionPrice, PostStreamActor}
import com.cluda.coinsignals.streams.protocoll.{NewStream, NewStreamJsonProtocol}
import com.cluda.coinsignals.streams.util.HttpUtil
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json._
import DefaultJsonProtocol._

trait Service {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  implicit val timeout: Timeout

  def config: Config
  val logger: LoggingAdapter
  val streamsTableName: String
  val runID = UUID.randomUUID()
  var actorIDs = Map[String, Long]()



  def actorName(props: Props): String = {
    val classPath = props.actorClass().toString
    val className = classPath.substring(classPath.lastIndexOf('.') + 1)
    val id: Long = actorIDs.getOrElse(className, 0)
    if (id == 0) {
      actorIDs = actorIDs+(className -> 1)
    }
    else {
      actorIDs = actorIDs+(className -> (id + 1))
    }
    className + id
  }

  /**
   * Start a actor and pass it the decodedHttpRequest.
   * Returns a future. If anything fails it returns HttpResponse with "BadRequest",
   * else it returns the HttpResponse returned by the started actor
   *
   * @param props of the actor to start
   * @return Future[HttpResponse]
   */
  def perRequestActor[T](props: Props, message: T): Future[HttpResponse] = {
    (system.actorOf(props, actorName(props)) ? message)
      .recover { case _ => HttpResponse(InternalServerError, entity = "InternalServerError") }
      .asInstanceOf[Future[HttpResponse]]
  }


  val routes = {
    headerValueByName("Global-Request-ID") { globalRequestID =>

      pathPrefix("ping") {
        complete {
          logger.info(s"[$globalRequestID]: Answering ping request")
          HttpResponse(OK, entity = Map("runID" -> runID.toString, "globalRequestID" -> globalRequestID).toJson.prettyPrint)
        }
      } ~
        pathPrefix("streams") {
          pathPrefix(Segment) { streamID =>
            get {
              parameters('private.as[Boolean].?) { privateInfo =>
                complete {
                  logger.info(s"[$globalRequestID]: Received get stream ($streamID) request.")
                  perRequestActor[(String, Boolean)](GetStreamsActor.props(globalRequestID, streamsTableName),
                  (streamID, privateInfo.getOrElse(false)))
                }
              }
            } ~
              pathPrefix("subscription-price") {
                post {
                  entity(as[String]) { newPriceString =>
                    try {
                      val newPrice = BigDecimal(newPriceString)
                      logger.info(s"[$globalRequestID]: Received post new subscription-price for stream ($streamID). " +
                        s"New price: $newPrice.")
                      complete(
                        perRequestActor[ChangeSubscriptionPrice](PostStreamActor.props(globalRequestID, streamsTableName),
                      ChangeSubscriptionPrice(streamID, newPrice))
                      )
                    }
                    catch {
                      case e: Throwable =>
                        logger.error(s"[$globalRequestID]: Received unknown POST subscription-price: $newPriceString. " +
                          s"Returning 'BadRequest'. For stream: $streamID. Error: " + e.toString)
                        complete(HttpResponse(BadRequest, entity = "BadRequest"))
                    }
                  }
                }
              } ~
              pathPrefix("signals") {
                post {
                  entity(as[String]) { newSignalsString =>
                    try {
                      import SignalJsonProtocol._
                      val signals = newSignalsString.parseJson.convertTo[Seq[Signal]]
                      logger.info(s"[$globalRequestID]: Received post new signal(s) for stream ($streamID). " +
                        s"Signals:" + signals.toJson.compactPrint)
                      complete {
                        import SignalJsonProtocol._
                        perRequestActor[Seq[Signal]](PostSignalActor.props(globalRequestID, streamID, streamsTableName),
                      signals)
                      }
                    }
                    catch {
                      case e: Throwable =>
                        logger.error(s"[$globalRequestID]: Received unknown POST signal(s): $newSignalsString. " +
                          s"Returning 'BadRequest'. For stream: $streamID. Error: " + e.toString)
                        complete(HttpResponse(BadRequest, entity = "BadRequest"))
                    }
                  }
                }
              }
          } ~
            post {
              entity(as[String]) { streamString =>
                try {
                  import NewStreamJsonProtocol._
                  val newStream: NewStream = streamString.parseJson.convertTo[NewStream]
                  logger.info(s"[$globalRequestID]: Received post new stream: " + newStream.toJson.compactPrint)
                  complete {
                    perRequestActor[NewStream](PostStreamActor.props(globalRequestID, streamsTableName), newStream)
                  }
                }
                catch {
                  case e: Throwable =>
                    logger.error(s"[$globalRequestID]: Received unknown POST stream-type: $streamString. " +
                      s"Returning 'BadRequest'. Error: " + e.toString)
                    complete(HttpResponse(BadRequest, entity = "BadRequest"))
                }
              }
            } ~
            get {
              complete {
                perRequestActor[String](GetStreamsActor.props(globalRequestID, streamsTableName), "all")
              }
            }
        }
    }
  }


}
