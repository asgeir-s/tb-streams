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
  def confirmAwsSnsSubscription(subscribeURL: String): Future[HttpResponse] = {

    logger.info("subscribeURL: " + subscribeURL)

    val splitPoint = subscribeURL.indexOf(".com") + 4
    val host = subscribeURL.substring(0, splitPoint)
    val path = subscribeURL.substring(splitPoint, subscribeURL.length)
    val https = true

    logger.info("host: " + host, ", path: " + path)

    HttpUtil.request(
      system,
      HttpMethods.GET,
      https,
      host,
      path
    )

  }


  val routes = {
    import spray.json._
    import DefaultJsonProtocol._

    pathPrefix("ping") {
      complete {
        HttpResponse(OK, entity = Map("runID" -> runID.toString).toJson.prettyPrint)
      }
    } ~
      pathPrefix("streams") {
        pathPrefix(Segment) { streamID =>
          get {
            parameters('private.as[Boolean].?) { privateInfo =>
              complete {
                perRequestActor[(String, Boolean)](GetStreamsActor.props(streamsTableName), (streamID, privateInfo.getOrElse(false)))
              }
            }
          } ~
            pathPrefix("subscription-price") {
              post {
                entity(as[String]) { newPriceString =>
                  val newPrice = BigDecimal(newPriceString)
                  complete(
                    perRequestActor[ChangeSubscriptionPrice](PostStreamActor.props(streamsTableName), ChangeSubscriptionPrice(streamID, newPrice))
                  )
                }
              }
            } ~
            pathPrefix("signals") {
              post {
                entity(as[String]) { bodyString =>
                  import spray.json._
                  val json = bodyString.parseJson
                  complete {
                    import SignalJsonProtocol._
                    perRequestActor[Seq[Signal]](PostSignalActor.props(streamID, streamsTableName), json.convertTo[Seq[Signal]])
                  }
                }
              }
            }
        } ~
          post {
            entity(as[String]) { streamString =>
              import NewStreamJsonProtocol._
              val newStream: NewStream = streamString.parseJson.convertTo[NewStream]
              complete {
                perRequestActor[NewStream](PostStreamActor.props(streamsTableName), newStream)
              }
            }
          } ~
          get {
            complete {
              perRequestActor[String](GetStreamsActor.props(streamsTableName), "all")
            }
          }
      }
  }


}
