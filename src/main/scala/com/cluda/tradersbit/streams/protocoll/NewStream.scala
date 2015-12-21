package com.cluda.tradersbit.streams.protocoll

import spray.json.DefaultJsonProtocol

case class NewStream(name: String, exchange: String, currencyPair: String, payoutAddress: String, subscriptionPriceUSD: BigDecimal)

object NewStreamJsonProtocol extends DefaultJsonProtocol {
  implicit val newStreamFormat = jsonFormat5(NewStream)
}
