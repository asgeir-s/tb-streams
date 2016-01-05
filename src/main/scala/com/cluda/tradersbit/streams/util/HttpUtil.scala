package com.cluda.tradersbit.streams.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.collection.immutable.SortedMap
import scala.concurrent.Future

object HttpUtil {

  def request(
    method: HttpMethod,
    isHttps: Boolean,
    baseUri: String,
    uri: String,
    body: String = "",
    headers: List[HttpHeader] = List(RawHeader("Accept", "*/*")),
    params: Map[String, String] = Map()
    )(implicit system: ActorSystem): Future[HttpResponse] = {
    implicit val materializer = ActorMaterializer()

    val _outgoingConn = if (isHttps) {
      Http().outgoingConnectionTls(baseUri)
    } else {
      Http().outgoingConnection(baseUri)
    }


    val entity: Option[RequestEntity] = {
      if (body == "") {
        None
      }
      else {
        Some(HttpEntity(ContentTypes.`application/json`, body))
      }
    }

    var queryParams = ""
    if (params.nonEmpty) {
      queryParams = "?" + getFormURLEncoded(params)
    }

    Source.single(HttpRequest(uri = uri + queryParams,
      method = method,
      headers = headers,
      entity = entity.getOrElse(HttpEntity.Empty)
    )
    ).via(_outgoingConn).runWith(Sink.head)
  }

  private def getFormURLEncoded(params: Map[String, String]): String = {
    val sortedParams = SortedMap(params.toList: _*)
    sortedParams.map { paramTuple =>
      java.net.URLEncoder.encode(paramTuple._1, "UTF-8") + "=" + java.net.URLEncoder.encode(paramTuple._2, "UTF-8")
    }.mkString("&")
  }
}
