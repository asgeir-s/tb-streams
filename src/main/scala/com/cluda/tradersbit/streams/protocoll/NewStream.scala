package com.cluda.tradersbit.streams.protocoll

import spray.json.DefaultJsonProtocol

case class NewStream(exchange: String, currencyPair: String, payoutAddress: String, subscriptionPriceUSD: BigDecimal)

object NewStreamJsonProtocol extends DefaultJsonProtocol {
  implicit val newStreamFormat = jsonFormat4(NewStream)
}
