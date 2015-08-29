package com.cluda.coinsignals.streams.model

import spray.json.DefaultJsonProtocol

object StreamStatsProtocol extends DefaultJsonProtocol {
  implicit val streamStatsFormat = jsonFormat22(StreamStats)
  implicit val streamPrivateFormat = jsonFormat3(StreamPrivate)
}

case class SStream(
  id: String,
  exchange: String,
  currencyPair: String,
  status: Int,
  idOfLastSignal: Long,
  subscriptionPriceUSD: BigDecimal,
  streamPrivate: StreamPrivate,
  stats: StreamStats = StreamStats(),
  computeComponents: ComputeComponents = ComputeComponents()
  ) {
  def publicJson: String = {
    import spray.json._
    import StreamStatsProtocol._
    val json = """{ "id": """" + id + """",""" +
      """ "exchange": """" + exchange + """",""" +
      """ "idOfLastSignal": """" + idOfLastSignal + """",""" +
    """ "currencyPair": """" + currencyPair + """",""" +
      """ "subscriptionPriceUSD": """" + subscriptionPriceUSD + """",""" +
      """ "stats": """ + stats.toJson.compactPrint + """}"""
    json
  }

  def privateJson: String = {
    import StreamStatsProtocol._
    import spray.json._
    val json = """{ "id": """" + id + """",""" +
      """ "exchange": """" + exchange + """",""" +
      """ "currencyPair": """" + currencyPair + """",""" +
      """ "subscriptionPriceUSD": """" + subscriptionPriceUSD + """",""" +
      """ "status": """" + status + """",""" +
      """ "streamPrivate": """ + streamPrivate.toJson.prettyPrint + """,""" +
      """ "stats": """ + stats.toJson.prettyPrint + """}"""
    json
  }
}

case class StreamPrivate(
  apiKey: String,
  topicArn: String,
  payoutAddress: String
  )

case class StreamStats(
  timeOfFirstSignal: Long = 0,
  timeOfLastSignal: Long = 0,
  numberOfSignals: Long = 0,
  numberOfClosedTrades: Long = 0,
  numberOfProfitableTrades: Long = 0,
  numberOfLoosingTrades: Long = 0,
  accumulatedProfit: BigDecimal = 0,
  accumulatedLoss: BigDecimal = 0,
  averageTrade: BigDecimal = 0,
  partWinningTrades: BigDecimal = 0,
  partLoosingTrades: BigDecimal = 0,
  profitFactor: BigDecimal = 0,
  buyAndHoldChange: BigDecimal = 0,
  averageWinningTrade: BigDecimal = 0,
  averageLoosingTrade: BigDecimal = 0,
  averageMonthlyProfitIncl: BigDecimal = 0,
  averageMonthlyProfitExcl: BigDecimal = 0,
  monthsOfTrading: BigDecimal = 0,
  maxDrawDown: BigDecimal = 0,
  allTimeValueIncl: BigDecimal = 1,
  allTimeValueExcl: BigDecimal = 1,
  firstPrice: BigDecimal = 0
  )

case class ComputeComponents(
  maxDDPrevMax: BigDecimal = 1,
  maxDDPrevMin: BigDecimal = 1,
  maxDDMax: BigDecimal = 1
  )

case class stat[A](name: String, value: A, printable: String, description: String)