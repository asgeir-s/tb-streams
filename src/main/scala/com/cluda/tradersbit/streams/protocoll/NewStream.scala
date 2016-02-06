package com.cluda.tradersbit.streams.protocoll

import spray.json.DefaultJsonProtocol

case class NewStream(name: String, exchange: String, currencyPair: String, payoutAddress: String, subscriptionPriceUSD: BigDecimal, userId: String)

object NewStreamJsonProtocol extends DefaultJsonProtocol {
  implicit val newStreamFormat = jsonFormat6(NewStream)
}
