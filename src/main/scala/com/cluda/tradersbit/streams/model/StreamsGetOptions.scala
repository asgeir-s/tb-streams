package com.cluda.tradersbit.streams.model

import spray.json.DefaultJsonProtocol

/**
  * Created by sogasg on 12/12/15.
  */
case class StreamsGetOptions(streams: List[String], infoLevel: Option[String], notArray: Option[Boolean])

object StreamsGetOptionsJsonProtocol extends DefaultJsonProtocol {
  implicit val streamsGetOptionsFormat = jsonFormat3(StreamsGetOptions)
}