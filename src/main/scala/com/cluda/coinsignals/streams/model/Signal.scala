package com.cluda.coinsignals.streams.model

import spray.json.DefaultJsonProtocol

/**
 *
 * @param signal 1 = long, 0 = close, -1 = short
 */
case class Signal(
  id: Long,
  signal: Int,
  timestamp: Long,
  price: BigDecimal,
  change: BigDecimal,
  value: BigDecimal)

object SignalJsonProtocol extends DefaultJsonProtocol {
  implicit val signalFormat = jsonFormat6(Signal)
}