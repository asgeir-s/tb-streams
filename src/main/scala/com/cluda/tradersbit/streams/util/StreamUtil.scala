package com.cluda.tradersbit.streams.util

import com.cluda.tradersbit.streams.model._

/**
  * lots of stats should only be updated when 'isClosingTrade'.
  * Because if not it will be possible to see when positions are opened from public data.
  */

object StreamUtil {

  def updateStreamWitheNewSignal(stream: SStream, signal: Signal): SStream = {
    val isClosingTrade: Boolean = signal.signal == 0

    val tradeResult: BigDecimal =
      if (isClosingTrade) {
        stream.lastSignal match {
          case Some(lastSignal: Signal) => lastSignal.changeInclFee + signal.changeInclFee
          case None => signal.changeInclFee
        }
      }
      else {
        BigDecimal(0)
      }

    val isProfitableTrade = tradeResult > 0

    val isLosingTrade = tradeResult < 0

    val firstPrice =
      if (stream.stats.numberOfSignals == 0) {
        Some(signal.price)
      }
      else {
        None
      }

    val allTimeValueIncl: Option[BigDecimal] = if (isClosingTrade) Some(signal.valueInclFee) else None

    val allTimeValueExcl: Option[BigDecimal] = if (isClosingTrade) Some(signal.value) else None

    val numberOfClosedTrades =
      if (stream.stats.numberOfSignals < 1) {
        Some(0l)
      }
      else if (isClosingTrade) {
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
      if (isProfitableTrade) {
        Some(stream.stats.numberOfProfitableTrades + 1)
      }
      else {
        None
      }

    val accumulatedProfit = {
      if (isProfitableTrade) {
        Some(stream.stats.accumulatedProfit + tradeResult)
      }
      else {
        None
      }
    }

    val numberOfLoosingTrades =
      if (isLosingTrade) {
        Some(stream.stats.numberOfLoosingTrades + 1)
      }
      else {
        None
      }

    val accumulatedLoss = {
      if (isLosingTrade) {
        Some(stream.stats.accumulatedLoss - tradeResult)
      }
      else {
        None
      }
    }

    val allTimeValueInclMDD = allTimeValueIncl.getOrElse(stream.stats.allTimeValueIncl)
    // max draw down
    val cComponents: ComputeComponents =
      if (stream.stats.numberOfSignals == 0) {
        ComputeComponents(allTimeValueInclMDD, allTimeValueInclMDD, 1)
      }
      else if (allTimeValueInclMDD > stream.computeComponents.maxDDMax) {
        val maxDDMax = allTimeValueInclMDD
        ComputeComponents(stream.computeComponents.maxDDPrevMax, stream.computeComponents.maxDDPrevMin, maxDDMax)
      }
      else if ((stream.computeComponents.maxDDMax - allTimeValueInclMDD) >
        (stream.computeComponents.maxDDPrevMax - stream.computeComponents.maxDDPrevMin)) {

        val maxDDPrevMax = stream.computeComponents.maxDDMax
        val maxDDPrevMin = allTimeValueInclMDD
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

    val buyAndHoldChange: Option[BigDecimal] =
      if (isClosingTrade) {
        if (signal.price - firstPrice.getOrElse(stream.stats.firstPrice) == BigDecimal(0)) {
          Some(BigDecimal(0))
        } else {
          Some(1 / firstPrice.getOrElse(stream.stats.firstPrice) *
            (signal.price - firstPrice.getOrElse(stream.stats.firstPrice)))
        }
      }
      else {
        None
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
      buyAndHoldChange = buyAndHoldChange.getOrElse(stream.stats.buyAndHoldChange),
      maxDrawDown = maxDrawDown,
      firstPrice = firstPrice.getOrElse(stream.stats.firstPrice),
      allTimeValueExcl = allTimeValueExcl.getOrElse(stream.stats.allTimeValueExcl),
      allTimeValueIncl = allTimeValueIncl.getOrElse(stream.stats.allTimeValueIncl)
    )

    SStream(
      stream.id,
      stream.name,
      stream.exchange,
      stream.currencyPair,
      signal.signal,
      signal.id,
      stream.subscriptionPriceUSD,
      StreamPrivate(
        stream.streamPrivate.apiKeyId,
        stream.streamPrivate.topicArn,
        stream.streamPrivate.payoutAddress),
      Some(signal),
      adoptedStreamStats,
      cComponents)
  }

  def checkRoundedEqualityExceptApiKeyAndIDAndNameAndLastSignal(stream1: SStream, stream2: SStream): Boolean = {
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
        //stream1.id == stream2.id &&
        stream1.idOfLastSignal == stream2.idOfLastSignal &&
        stream1.status == stream2.status &&
        stream1.stats.accumulatedLoss.toDouble == stream2.stats.accumulatedLoss.toDouble &&
        stream1.stats.accumulatedProfit.toDouble == stream2.stats.accumulatedProfit.toDouble &&
        stream1.stats.allTimeValueExcl.toDouble == stream2.stats.allTimeValueExcl.toDouble &&
        stream1.stats.allTimeValueIncl.toDouble == stream2.stats.allTimeValueIncl.toDouble &&
        stream1.stats.buyAndHoldChange.toDouble == stream2.stats.buyAndHoldChange.toDouble &&
        stream1.stats.firstPrice.toDouble == stream2.stats.firstPrice.toDouble &&
        stream1.stats.maxDrawDown.toDouble == stream2.stats.maxDrawDown.toDouble &&
        stream1.stats.numberOfClosedTrades == stream2.stats.numberOfClosedTrades &&
        stream1.stats.numberOfLoosingTrades == stream2.stats.numberOfLoosingTrades &&
        stream1.stats.numberOfProfitableTrades == stream2.stats.numberOfProfitableTrades &&
        stream1.stats.numberOfSignals == stream2.stats.numberOfSignals &&
        stream1.stats.timeOfFirstSignal == stream2.stats.timeOfFirstSignal &&
        stream1.stats.timeOfLastSignal == stream2.stats.timeOfLastSignal &&
        stream1.computeComponents.maxDDMax.toDouble == stream2.computeComponents.maxDDMax.toDouble &&
        stream1.computeComponents.maxDDPrevMax.toDouble == stream2.computeComponents.maxDDPrevMax.toDouble &&
        stream1.computeComponents.maxDDPrevMin.toDouble == stream2.computeComponents.maxDDPrevMin.toDouble
    }


  }

}
