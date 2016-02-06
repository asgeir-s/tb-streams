package com.cluda.tradersbit.streams.model

import spray.json.DefaultJsonProtocol

object StreamStatsProtocol extends DefaultJsonProtocol {
  implicit val streamStatsFormat = jsonFormat13(StreamStats)
  implicit val streamPrivateFormat = jsonFormat4(StreamPrivate)
}

case class SStream(
  id: Option[String],
  name: String,
  exchange: String,
  currencyPair: String,
  status: Int,
  idOfLastSignal: Long,
  subscriptionPriceUSD: BigDecimal,
  streamPrivate: StreamPrivate,
  lastSignal: Option[Signal] = None,
  stats: StreamStats = StreamStats(),
  computeComponents: ComputeComponents = ComputeComponents()
  ) {
  def publicJson: String = {
    import spray.json._
    import StreamStatsProtocol._
    val json = """{ "id": """" + id.get + """",""" +
      """ "name": """" + name + """",""" +
      """ "exchange": """" + exchange + """",""" +
      """ "currencyPair": """" + currencyPair + """",""" +
      """ "subscriptionPriceUSD": """ + subscriptionPriceUSD + """,""" +
      """ "stats": """ + stats.toJson.compactPrint + """}"""
    json.parseJson.prettyPrint
  }

  def publisherJson: String = {
    import StreamStatsProtocol._
    import spray.json._
    val json = """{ "id": """" + id.get + """",""" +
      """ "name": """" + name + """",""" +
      """ "exchange": """" + exchange + """",""" +
      """ "currencyPair": """" + currencyPair + """",""" +
      """ "subscriptionPriceUSD": """ + subscriptionPriceUSD + """,""" +
      """ "status": """ + status + """,""" +
      """ "idOfLastSignal": """ + idOfLastSignal + """,""" +
      """ "lastSignal": """ + getLastSignal + """,""" +
      """ "payoutAddress": """" + streamPrivate.payoutAddress + """",""" +
      """ "stats": """ + stats.toJson.prettyPrint + """}"""
    json.parseJson.prettyPrint
  }

  def privateJson: String = {
    import StreamStatsProtocol._
    import spray.json._
    val json = """{ "id": """" + id.get + """",""" +
      """ "name": """" + name + """",""" +
      """ "exchange": """" + exchange + """",""" +
      """ "currencyPair": """" + currencyPair + """",""" +
      """ "subscriptionPriceUSD": """ + subscriptionPriceUSD + """,""" +
      """ "status": """ + status + """,""" +
      """ "idOfLastSignal": """ + idOfLastSignal + """,""" +
      """ "lastSignal": """ + getLastSignal + """,""" +
      """ "streamPrivate": """ + streamPrivate.toJson.prettyPrint + """,""" +
      """ "stats": """ + stats.toJson.prettyPrint + """}"""
    json.parseJson.prettyPrint
  }

  def getLastSignal = {
    import SignalJsonProtocol._
    import StreamStatsProtocol._
    import spray.json._

    if(lastSignal.isDefined) {
      lastSignal.get.toJson
    }
    else {
      "{}".parseJson
    }
  }

  def sStreamWithId(id: String) = {
    SStream(Some(id), name, exchange, currencyPair, status, idOfLastSignal, subscriptionPriceUSD, streamPrivate, lastSignal, stats, computeComponents)
  }
}

case class StreamPrivate(
  apiKeyId: String,
  topicArn: String,
  payoutAddress: String,
  userId: String
  )

case class StreamStats(
  timeOfFirstSignal: Long = 0,
  timeOfLastSignal: Long = 0,
  numberOfSignals: Long = 0,
  numberOfClosedTrades: Long = 0,
  numberOfProfitableTrades: Long = 0,
  numberOfLoosingTrades: Long = 0,
  accumulatedProfit: BigDecimal = 0,  // do not display
  accumulatedLoss: BigDecimal = 0,  // do not display
  //averageTrade: BigDecimal = 0,  // remove should be computed from (numberOfClosedTrades,allTimeValueIncl)
  //partWinningTrades: BigDecimal = 0,  // remove should be computed from (numberOfClosedTrades,numberOfProfitableTrades)
  //partLoosingTrades: BigDecimal = 0,  // remove should be computed from (numberOfClosedTrades,numberOfLoosingTrades)
  //profitFactor: BigDecimal = 0, // remove should be computed from (accumulatedProfit/accumulatedLoss)
  buyAndHoldChange: BigDecimal = 0,
  //averageWinningTrade: BigDecimal = 0, // remove should be computed from (accumulatedProfit,numberOfProfitableTrades)
  //averageLoosingTrade: BigDecimal = 0, // remove should be computed from (accumulatedLoss,numberOfLoosingTrades)
  //averageMonthlyProfitIncl: BigDecimal = 0, // remove should be computed from (allTimeValueIncl,timeOfFirstSignal,timeOfLastSignal)
  //averageMonthlyProfitExcl: BigDecimal = 0, // remove should be computed from (allTimeValueExcl,timeOfFirstSignal,timeOfLastSignal)
  //monthsOfTrading: BigDecimal = 0, // remove should be computed from (timeOfFirstSignal,timeOfLastSignal)
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