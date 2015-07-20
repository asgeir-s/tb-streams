package com.cluda.coinsignals.streams

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.RouteResult.Rejected
import com.cluda.coinsignals.protocol.Sec
import com.cluda.coinsignals.protocol.Sec._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.cluda.coinsignals.streams.getstream.GetStreamsActor
import com.cluda.coinsignals.streams.model.{Signal, SignalJsonProtocol}
import com.cluda.coinsignals.streams.postsignal.PostSignalActor
import com.cluda.coinsignals.streams.poststream.PostStreamActor
import com.cluda.coinsignals.streams.protocoll.{NewStream, NewStreamJsonProtocol}
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

  val authHeaderName = config.getString("auth.haderName")

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
      .recover { case _ => secureHttpResponse(BadRequest, entity = "BadRequest") }
      .asInstanceOf[Future[HttpResponse]]
  }

  def confirmAwsSnsSubscription(subscribeURL: String): Future[HttpResponse] = {
    val splitPoint = subscribeURL.indexOf(".com") + 4
    val host = subscribeURL.substring(0, splitPoint)
    val path = subscribeURL.substring(splitPoint, subscribeURL.length)

    val conn = Http().outgoingConnection(host)
    val request = HttpRequest(GET, uri = path)
    Source.single(request).via(conn).runWith(Sink.head[HttpResponse])
  }

  val routes = {
    headerValueByName(authHeaderName) { auth =>
      if (authRequest(auth)) {
        pathPrefix("streams") {
          pathPrefix(Segment) { streamID =>
            get {
              parameters('private.as[Boolean].?) { privateInfo =>
                complete {
                  perRequestActor[(String, Boolean)](GetStreamsActor.props(streamsTableName), (streamID, privateInfo.getOrElse(false)))
                }
              }
            }
          } ~
            post {
              import NewStreamJsonProtocol._
              import spray.json._
              entity(as[String]) { message =>
                val streamStringOpt = validateAndDecryptMessage(message)

                if (streamStringOpt.isDefined) {
                  val streamString = streamStringOpt.get
                  val newStream: NewStream = streamString.parseJson.convertTo[NewStream]
                  complete {
                    perRequestActor[NewStream](PostStreamActor.props(streamsTableName), newStream)
                  }
                }
                else {
                  complete(secureHttpResponse(BadRequest, entity = "BadRequest"))
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
      else {
        reject
      }
    } ~
      headerValueByName("x-amz-sns-message-type") { awsMessageType =>
        pathPrefix("streams") {
          pathPrefix(Segment) { streamID =>
            pathPrefix("signals") {
              post {
                entity(as[String]) { bodyString =>
                  import spray.json._
                  // if header: 'x-amz-sns-message-type: SubscriptionConfirmation'
                  if (awsMessageType == "SubscriptionConfirmation") {
                    val confirmUrl = bodyString.parseJson.asJsObject.getFields("SubscribeURL").head.toString()
                      .replaceAll( """"""", "")
                      .replace("https://", "")
                      .replace("http://", "")
                    complete(
                      confirmAwsSnsSubscription(confirmUrl)
                    )
                  }
                  else {
                    import SignalJsonProtocol._
                    if (awsMessageType == "Notification") {
                      val message = bodyString.parseJson.asJsObject.getFields("Message").head.toString()
                      val decoded = message.replace( """\n""", " ").replace( """\""", "")
                      val removeFirstAndLAst = decoded.substring(1, decoded.length - 1)
                      val validated = Sec.validateAndDecryptMessage(removeFirstAndLAst)

                      val json = validated.getOrElse("").parseJson
                      complete {
                        perRequestActor[Seq[Signal]](PostSignalActor.props(streamID, streamsTableName), json.convertTo[Seq[Signal]])
                      }
                    }
                    else {
                      reject
                    }
                  }

                }
              }
            }
          }
        }

      }
  }


}
