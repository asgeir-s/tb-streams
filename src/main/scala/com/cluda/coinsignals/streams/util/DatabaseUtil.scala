package com.cluda.coinsignals.streams.util

import awscala.dynamodbv2._
import com.amazonaws.regions.Region
import com.cluda.coinsignals.streams.model.{StreamPrivate, ComputeComponents, SStream, StreamStats}
import com.cluda.coinsignals.streams.protocoll.NewStream
import com.typesafe.config.Config

object DatabaseUtil {

  def awscalaDB(config: Config):  DynamoDB = {
    implicit val region: Region = awscala.Region(config.getString("aws.dynamo.region"))
    val awscalaCredentials = awscala.BasicCredentialsProvider(
      config.getString("aws.accessKeyId"),
      config.getString("aws.secretAccessKey"))

    awscala.dynamodbv2.DynamoDB(awscalaCredentials)
  }

  /**
   *
   * @param table streamsTable
   * @param stream id of the stream
   * @return
   */
  def putStream(implicit dynamoDB: DynamoDB, table: Table, stream: SStream): String = {
    table.put(
      stream.id,
      "exchange" -> stream.exchange,
      "currencyPair" -> stream.currencyPair,
      "apiKey" -> stream.streamPrivate.apiKey,
      "topicArn" -> stream.streamPrivate.topicArn,
      "status" -> stream.status,
      "idOfLastSignal" -> stream.idOfLastSignal,

      "timeOfFirstSignal" -> stream.stats.timeOfFirstSignal,
      "timeOfLastSignal" -> stream.stats.timeOfLastSignal,
      "numberOfSignals" -> stream.stats.numberOfSignals,
      "numberOfClosedTrades" -> stream.stats.numberOfClosedTrades,
      "numberOfProfitableTrades" -> stream.stats.numberOfProfitableTrades,
      "numberOfLoosingTrades" -> stream.stats.numberOfLoosingTrades,
      "accumulatedProfit" -> stream.stats.accumulatedProfit,
      "accumulatedLoss" -> stream.stats.accumulatedLoss,
      "averageTrade" -> stream.stats.averageTrade,
      "partWinningTrades" -> stream.stats.partWinningTrades,
      "partLoosingTrades" -> stream.stats.partLoosingTrades,
      "profitFactor" -> stream.stats.profitFactor,
      "buyAndHoldChange" -> stream.stats.buyAndHoldChange,
      "averageWinningTrade" -> stream.stats.averageWinningTrade,
      "averageLoosingTrade" -> stream.stats.averageLoosingTrade,
      "averageMonthlyProfitIncl" -> stream.stats.averageMonthlyProfitIncl,
      "averageMonthlyProfitExcl" -> stream.stats.averageMonthlyProfitExcl,
      "monthsOfTrading" -> stream.stats.monthsOfTrading,
      "maxDrawDown" -> stream.stats.maxDrawDown,
      "allTimeValueIncl" -> stream.stats.allTimeValueIncl,
      "allTimeValueExcl" -> stream.stats.allTimeValueExcl,
      "firstPrice" -> stream.stats.firstPrice,
    
      "maxDDPrevMax" -> stream.computeComponents.maxDDPrevMax,
      "maxDDPrevMin" -> stream.computeComponents.maxDDPrevMin,
      "maxDDMax" -> stream.computeComponents.maxDDMax
    )
    stream.id
  }

  /**
   *
   * @param table streamsTable
   * @param newStream id of the stream
   * @return
   */
  def putNewStream(implicit dynamoDB: DynamoDB, table: Table, newStream: NewStream, topicArn: String): String = {
    putStream(dynamoDB: DynamoDB, table: Table, SStream(newStream.id, newStream.exchange, newStream.currencyPair, 0, 0, StreamPrivate(newStream.apiKey, topicArn)))
  }


  /**
   *
   * @param table streamsTable
   * @param streamID the stream corresponding to the id
   * @return
   */
  def getStream(implicit dynamoDB: DynamoDB, table: Table, streamID: String): Option[SStream] = {

    val streamFromDb = table.getItem(streamID)
    if (streamFromDb.isDefined) {
      val streamGotten = streamFromDb.get
      val attrMap = streamGotten.attributes.map(x => (x.name, x.value.s.getOrElse(x.value.n.getOrElse("")))).toMap

      val cComponents = ComputeComponents(
        maxDDPrevMax = BigDecimal(attrMap("maxDDPrevMax")),
        maxDDPrevMin = BigDecimal(attrMap("maxDDPrevMin")),
        maxDDMax = BigDecimal(attrMap("maxDDMax"))
      )

      val stats = StreamStats(
        timeOfFirstSignal = attrMap("timeOfFirstSignal").toLong,
        timeOfLastSignal = attrMap("timeOfLastSignal").toLong,
        numberOfSignals = attrMap("numberOfSignals").toLong,
        numberOfClosedTrades = attrMap("numberOfClosedTrades").toLong,
        numberOfProfitableTrades = attrMap("numberOfProfitableTrades").toLong,
        numberOfLoosingTrades = attrMap("numberOfLoosingTrades").toLong,
        accumulatedProfit = BigDecimal(attrMap("accumulatedProfit")),
        accumulatedLoss = BigDecimal(attrMap("accumulatedLoss")),
        averageTrade = BigDecimal(attrMap("averageTrade")),
        partWinningTrades = BigDecimal(attrMap("partWinningTrades")),
        partLoosingTrades = BigDecimal(attrMap("partLoosingTrades")),
        profitFactor = BigDecimal(attrMap("profitFactor")),
        buyAndHoldChange = BigDecimal(attrMap("buyAndHoldChange")),
        averageWinningTrade = BigDecimal(attrMap("averageWinningTrade")),
        averageLoosingTrade = BigDecimal(attrMap("averageLoosingTrade")),
        averageMonthlyProfitIncl = BigDecimal(attrMap("averageMonthlyProfitIncl")),
        averageMonthlyProfitExcl = BigDecimal(attrMap("averageMonthlyProfitExcl")),
        monthsOfTrading = BigDecimal(attrMap("monthsOfTrading")),
        maxDrawDown = BigDecimal(attrMap("maxDrawDown")),
        allTimeValueIncl = BigDecimal(attrMap("allTimeValueIncl")),
        allTimeValueExcl = BigDecimal(attrMap("allTimeValueExcl")),
        firstPrice = BigDecimal(attrMap("firstPrice"))
      )

      val sPrivate = StreamPrivate(
        apiKey = attrMap("apiKey"),
        topicArn = attrMap("topicArn")
      )

      val stream = SStream(
        id = attrMap("id"),
        exchange = attrMap("exchange"),
        currencyPair = attrMap("currencyPair"),
        status = attrMap("status").toInt,
        idOfLastSignal = attrMap("idOfLastSignal").toLong,
        stats = stats,
        computeComponents = cComponents,
        streamPrivate = sPrivate
      )

      Some(stream)
    }
    else {
      None
    }

  }
}
