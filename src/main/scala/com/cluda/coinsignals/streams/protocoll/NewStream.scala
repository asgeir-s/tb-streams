package com.cluda.coinsignals.streams.protocoll

import spray.json.DefaultJsonProtocol

case class NewStream(id: String, exchange: String, currencyPair: String, apiKey: String)

object NewStreamJsonProtocol extends DefaultJsonProtocol {
  implicit val newStreamFormat = jsonFormat4(NewStream)
}
