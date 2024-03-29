package com.cluda.tradersbit.streams

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpHeader, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RequestContext
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.cluda.tradersbit.streams.model.{StreamsGetOptionsJsonProtocol, Signal, SignalJsonProtocol, StreamsGetOptions}
import com.cluda.tradersbit.streams.postsignal.PostSignalActor
import com.cluda.tradersbit.streams.poststream.{ChangeSubscriptionPrice, PostStreamActor}
import com.cluda.tradersbit.streams.protocoll.{NewStream, NewStreamJsonProtocol}
import com.typesafe.config.Config
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

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

  private lazy val apiKey = config.getString("microservices.streamsApiKey")


  def actorName(props: Props): String = {
    val classPath = props.actorClass().toString
    val className = classPath.substring(classPath.lastIndexOf('.') + 1)
    val id: Long = actorIDs.getOrElse(className, 0)
    if (id == 0) {
      actorIDs = actorIDs + (className -> 1)
    }
    else {
      actorIDs = actorIDs + (className -> (id + 1))
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

  def auth: RequestContext => Boolean = (ctx: RequestContext) => {
    ctx.request.headers.find(_.is("authorization")) match {
      case Some(header: HttpHeader) =>
        val thisApiKey = header.value().split(" ")(1)
        thisApiKey.equals(apiKey)
      case None => false
    }
  }

  val routes = {
    authorize(auth) {
      headerValueByName("Global-Request-ID") { globalRequestID =>

        pathPrefix("ping") {
          complete {
            logger.info(s"[$globalRequestID]: Answering ping request")
            HttpResponse(OK, entity = Map("runID" -> runID.toString, "globalRequestID" -> globalRequestID).toJson.prettyPrint)
          }
        } ~
          pathPrefix("streams") {
              pathPrefix(Segment) { streamID =>
                  pathPrefix("signals") {
                    post {
                      entity(as[String]) { newSignalsString =>
                        try {
                          import SignalJsonProtocol._
                          val signals = newSignalsString.parseJson.convertTo[Seq[Signal]]
                          logger.info(s"[$globalRequestID]: Received post new signal(s) for stream ($streamID). " +
                            s"Signals:" + signals.toJson.compactPrint)
                          complete {
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
              }
          }
      }
    }
  }


}
