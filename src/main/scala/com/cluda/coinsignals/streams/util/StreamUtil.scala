package com.cluda.coinsignals.streams.util

import com.cluda.coinsignals.streams.model._

object StreamUtil {

  val weekMs: Long = 604800000
  val twoWeekMs: Long = weekMs * 2
  val monthMs: Long = 2628000000l
  val threeMonthsMs: Long = monthMs * 3
  val sixMonthsMs: Long = threeMonthsMs * 2
  val yearMs: Long = sixMonthsMs * 2

  def updateStreamWitheNewSignal(stream: SStream, signal: Signal): SStream = {
    val isProfitable = signal.change > 0
    val isLosing = signal.change < 0

    val firstPrice =
      if (stream.stats.numberOfSignals == 0) {
        Some(signal.price)
      }
      else {
        None
      }

    val allTimeValueIncl: BigDecimal =
      (stream.stats.allTimeValueIncl - BigDecimal(0.0025)) * (BigDecimal(1) + signal.change)

    val allTimeValueExcl = signal.value

    val numberOfClosedTrades =
      if (stream.stats.numberOfSignals < 1) {
        Some(0l)
      }
      else if (signal.signal == 0) {
        Some(stream.stats.numberOfClosedTrades + 1)
      }
      else {
        None
      }

    val numberOfSignals = stream.stats.numberOfSignals + 1

    val timeOfFirstSignal =
      if (stream.stats.timeOfFirstSignal == 0) {
        Some(signal.timestamp)
      }
      else {
        None
      }

    val numberOfProfitableTrades =
      if (isProfitable) {
        Some(stream.stats.numberOfProfitableTrades + 1)
      }
      else {
        None
      }

    val accumulatedProfit = {
      if (isProfitable) {
        Some(stream.stats.accumulatedProfit + signal.change)
      }
      else {
        None
      }
    }

    val numberOfLoosingTrades =
      if (isLosing) {
        Some(stream.stats.numberOfLoosingTrades + 1)
      }
      else {
        None
      }

    val accumulatedLoss = {
      if (isLosing) {
        Some(stream.stats.accumulatedLoss - signal.change)
      }
      else {
        None
      }
    }

    // max draw down
    val cComponents: ComputeComponents =
      if (stream.stats.numberOfSignals == 0) {
        ComputeComponents(signal.value, signal.value, 1)
      }
      else if (signal.value > stream.computeComponents.maxDDMax) {
        val maxDDMax = signal.value
        ComputeComponents(stream.computeComponents.maxDDPrevMax, stream.computeComponents.maxDDPrevMin, maxDDMax)
      } else if ((stream.computeComponents.maxDDMax - signal.value) >
        (stream.computeComponents.maxDDPrevMax - stream.computeComponents.maxDDPrevMin)) {

        val maxDDPrevMax = stream.computeComponents.maxDDMax
        val maxDDPrevMin = signal.value
        ComputeComponents(maxDDPrevMax, maxDDPrevMin, stream.computeComponents.maxDDMax)
      }
      else {
        ComputeComponents(stream.computeComponents.maxDDPrevMax,
          stream.computeComponents.maxDDPrevMin, stream.computeComponents.maxDDMax)
      }

    val absMaxDrawDown = cComponents.maxDDPrevMax - cComponents.maxDDPrevMin

    val maxDrawDown: BigDecimal =
      if (cComponents.maxDDPrevMax == BigDecimal(0)) {
        BigDecimal(0)
      } else {
        (BigDecimal(1) / cComponents.maxDDPrevMax) * absMaxDrawDown
      }



    val streamDuration = signal.timestamp - timeOfFirstSignal.getOrElse(stream.stats.timeOfFirstSignal)

    val averageMonthlyProfitExcl: BigDecimal =
      if (streamDuration == 0) {
        BigDecimal(0)
      } else {
        ((allTimeValueExcl- BigDecimal(1)) / BigDecimal(streamDuration)) * BigDecimal(monthMs)
      }

    val averageMonthlyProfitIncl: BigDecimal =
      if (streamDuration == 0) {
        BigDecimal(0)
      } else {
        ((allTimeValueIncl - BigDecimal(1)) / BigDecimal(streamDuration)) * BigDecimal(monthMs)
      }

    val averageTrade: BigDecimal =
      if (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long] == 0) {
        BigDecimal(0)
      }
      else {
        (accumulatedProfit.getOrElse(stream.stats.accumulatedProfit) -
          accumulatedLoss.getOrElse(stream.stats.accumulatedLoss)) /
          BigDecimal(numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long])
      }

    val averageWinningTrade: BigDecimal =
      if (numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades).asInstanceOf[Long] == 0) {
        BigDecimal(0)
      } else {
        accumulatedProfit.getOrElse(stream.stats.accumulatedProfit) /
          BigDecimal(numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades).asInstanceOf[Long])
      }

    val averageLoosingTrade: BigDecimal =
      if (numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades).asInstanceOf[Long] == 0) {
        BigDecimal(0)
      } else {
        accumulatedLoss.getOrElse(stream.stats.accumulatedLoss) /
          BigDecimal(numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades).asInstanceOf[Long])
      }


    val partProfitableTrades: BigDecimal =
      if (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long] == 0) {
        BigDecimal(0)
      } else {
        BigDecimal(numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades)) /
          BigDecimal(numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long])
      }

    val partLoosingTrades: BigDecimal =
      if (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long] == 0) {
        BigDecimal(0)
      } else {
        BigDecimal(numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades)) /
          BigDecimal(numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long])
      }

    val profitFactor: BigDecimal =
      if (accumulatedLoss.getOrElse(stream.stats.accumulatedLoss) == BigDecimal(0)) {
        BigDecimal(0)
      } else {
        accumulatedProfit.getOrElse(stream.stats.accumulatedProfit) /
          accumulatedLoss.getOrElse(stream.stats.accumulatedLoss)

      }

    val buyAndHoldChange: BigDecimal =
      if (firstPrice.getOrElse(stream.stats.firstPrice) *
        (signal.price - firstPrice.getOrElse(stream.stats.firstPrice)) == BigDecimal(0)) {
        BigDecimal(0)
      } else {
        1 / firstPrice.getOrElse(stream.stats.firstPrice) *
          (signal.price - firstPrice.getOrElse(stream.stats.firstPrice))
      }

    val adoptedStreamStats = StreamStats(
      timeOfFirstSignal = timeOfFirstSignal.getOrElse(stream.stats.timeOfFirstSignal),
      timeOfLastSignal = signal.timestamp,
      numberOfSignals = numberOfSignals,
      numberOfClosedTrades = numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long],
      numberOfProfitableTrades = numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades).asInstanceOf[Long],
      numberOfLoosingTrades = numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades).asInstanceOf[Long],
      accumulatedProfit = accumulatedProfit.getOrElse(stream.stats.accumulatedProfit),
      accumulatedLoss = accumulatedLoss.getOrElse(stream.stats.accumulatedLoss),
      averageTrade = averageTrade,
      partWinningTrades = partProfitableTrades,
      partLoosingTrades = partLoosingTrades,
      profitFactor = profitFactor,
      buyAndHoldChange = buyAndHoldChange,
      averageWinningTrade = averageWinningTrade,
      averageLoosingTrade = averageLoosingTrade,
      averageMonthlyProfitIncl = averageMonthlyProfitIncl,
      averageMonthlyProfitExcl = averageMonthlyProfitExcl,
      monthsOfTrading = streamDuration / monthMs,
      maxDrawDown = maxDrawDown,
      firstPrice = firstPrice.getOrElse(stream.stats.firstPrice),
      allTimeValueExcl = allTimeValueExcl,
      allTimeValueIncl = allTimeValueIncl
    )

    SStream(
      stream.id,
      stream.exchange,
      stream.currencyPair, 
      signal.signal,
      signal.id,
      stream.subscriptionPriceUSD,
      StreamPrivate(
        stream.streamPrivate.apiKey,
        stream.streamPrivate.topicArn,
        stream.streamPrivate.payoutAddress),
      adoptedStreamStats,
      cComponents)
  }

  def checkRoundedEqualityExceptApiKey(stream1: SStream, stream2: SStream): Boolean = {
    if (stream1 == stream2) {
      true
    }
    else {
      stream1.streamPrivate.payoutAddress == stream2.streamPrivate.payoutAddress &&
      //stream1.streamPrivate.apiKey == stream2.streamPrivate.apiKey &&
        stream1.streamPrivate.topicArn == stream2.streamPrivate.topicArn &&
        stream1.computeComponents == stream2.computeComponents &&
        stream1.currencyPair == stream2.currencyPair &&
        stream1.exchange == stream2.exchange &&
        stream1.subscriptionPriceUSD == stream2.subscriptionPriceUSD &&
        stream1.id == stream2.id &&
        stream1.idOfLastSignal == stream2.idOfLastSignal &&
        stream1.status == stream2.status &&
        stream1.stats.accumulatedLoss.toDouble == stream2.stats.accumulatedLoss.toDouble &&
        stream1.stats.accumulatedProfit.toDouble == stream2.stats.accumulatedProfit.toDouble &&
        stream1.stats.allTimeValueExcl.toDouble == stream2.stats.allTimeValueExcl.toDouble &&
        stream1.stats.allTimeValueIncl.toDouble == stream2.stats.allTimeValueIncl.toDouble &&
        stream1.stats.averageLoosingTrade.toDouble == stream2.stats.averageLoosingTrade.toDouble &&
        stream1.stats.averageMonthlyProfitExcl.toDouble == stream2.stats.averageMonthlyProfitExcl.toDouble &&
        stream1.stats.averageMonthlyProfitIncl.toDouble == stream2.stats.averageMonthlyProfitIncl.toDouble &&
        stream1.stats.averageTrade.toDouble == stream2.stats.averageTrade.toDouble &&
        stream1.stats.averageWinningTrade.toDouble == stream2.stats.averageWinningTrade.toDouble &&
        stream1.stats.buyAndHoldChange.toDouble == stream2.stats.buyAndHoldChange.toDouble &&
        stream1.stats.firstPrice.toDouble == stream2.stats.firstPrice.toDouble &&
        stream1.stats.maxDrawDown.toDouble == stream2.stats.maxDrawDown.toDouble &&
        stream1.stats.monthsOfTrading.toDouble == stream2.stats.monthsOfTrading.toDouble &&
        stream1.stats.numberOfClosedTrades == stream2.stats.numberOfClosedTrades &&
        stream1.stats.numberOfLoosingTrades == stream2.stats.numberOfLoosingTrades &&
        stream1.stats.numberOfProfitableTrades == stream2.stats.numberOfProfitableTrades &&
        stream1.stats.numberOfSignals == stream2.stats.numberOfSignals &&
        stream1.stats.partLoosingTrades.toDouble == stream2.stats.partLoosingTrades.toDouble &&
        stream1.stats.partWinningTrades.toDouble == stream2.stats.partWinningTrades.toDouble &&
        stream1.stats.profitFactor.toDouble == stream2.stats.profitFactor.toDouble &&
        stream1.stats.timeOfFirstSignal == stream2.stats.timeOfFirstSignal &&
        stream1.stats.timeOfLastSignal == stream2.stats.timeOfLastSignal &&
        stream1.computeComponents.maxDDMax.toDouble == stream2.computeComponents.maxDDMax.toDouble &&
        stream1.computeComponents.maxDDPrevMax.toDouble == stream2.computeComponents.maxDDPrevMax.toDouble &&
        stream1.computeComponents.maxDDPrevMin.toDouble == stream2.computeComponents.maxDDPrevMin.toDouble
    }


  }

}
