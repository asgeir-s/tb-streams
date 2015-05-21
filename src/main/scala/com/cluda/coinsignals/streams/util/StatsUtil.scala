package com.cluda.coinsignals.streams.util

import com.cluda.coinsignals.streams.model.{ComputeComponents, SStream, Signal, StreamStats}

object StatsUtil {

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

    val allTimeProfitIncl = stream.stats.allTimeProfitExcl + (signal.change - 0.0025)

    val allTimeProfitExcl = signal.value

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
        Some(stream.stats.timeOfFirstSignal)
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

    val cComponents: ComputeComponents =
      if (stream.stats.numberOfSignals == 0) {
        ComputeComponents(signal.value, signal.value, 0)
      }
      else if (signal.value >= stream.computeComponents.maxDDMax) {
        val maxDDMax = stream.computeComponents.maxDDMax
        ComputeComponents(stream.computeComponents.maxDDPrevMax, stream.computeComponents.maxDDPrevMin, maxDDMax)
      } else if ((stream.computeComponents.maxDDMax - signal.value) > (stream.computeComponents.maxDDPrevMax - stream.computeComponents.maxDDPrevMin)) {
        val maxDDPrevMax = stream.computeComponents.maxDDMax
        val maxDDPrevMin = signal.value
        ComputeComponents(maxDDPrevMax, maxDDPrevMin, stream.computeComponents.maxDDMax)
      }
      else {
        ComputeComponents(stream.computeComponents.maxDDPrevMax, stream.computeComponents.maxDDPrevMin, stream.computeComponents.maxDDMax)
      }

    val absMaxDrawDown = cComponents.maxDDPrevMax - cComponents.maxDDPrevMin

    val maxDrawDown: BigDecimal =
    if(cComponents.maxDDPrevMax == 0) {
      0
    } else {
      (1 / cComponents.maxDDPrevMax) * absMaxDrawDown
    }

    val streamDuration = timeOfFirstSignal.getOrElse(stream.stats.timeOfFirstSignal) - signal.timestamp

    val averageMonthlyProfitExcl = (allTimeProfitExcl / streamDuration) * monthMs

    val allTimeProfitInclFees = (allTimeProfitIncl / streamDuration) * monthMs

    val averageTrade: BigDecimal =
      if (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long] == 0){
        0
      }
    else {
        (accumulatedProfit.getOrElse(stream.stats.accumulatedProfit) - accumulatedLoss.getOrElse(stream.stats.accumulatedLoss)) / (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long])
      }

    val averageWinningTrade: BigDecimal =
      if (numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades).asInstanceOf[Long] == 0) {
        0
      } else {
        accumulatedProfit.getOrElse(stream.stats.accumulatedProfit) / numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades).asInstanceOf[Long]
      }

    val averageLoosingTrade: BigDecimal =
      if (numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades).asInstanceOf[Long] == 0) {
        0
      } else {
        accumulatedLoss.getOrElse(stream.stats.accumulatedLoss) / numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades).asInstanceOf[Long]
      }


    val partProfitableTrades: BigDecimal =
      if (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long] == 0) {
        0
      } else {
        numberOfProfitableTrades.getOrElse(stream.stats.numberOfProfitableTrades) / numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long]

      }

    val partLoosingTrades: BigDecimal =
      if (numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long] == 0) {
        0
      } else {
        numberOfLoosingTrades.getOrElse(stream.stats.numberOfLoosingTrades) / numberOfClosedTrades.getOrElse(stream.stats.numberOfClosedTrades).asInstanceOf[Long]
      }

    val profitFactor: BigDecimal =
    if((-accumulatedLoss.getOrElse(stream.stats.accumulatedLoss)) == 0){
      0
    } else {
      accumulatedProfit.getOrElse(stream.stats.accumulatedProfit) / (-accumulatedLoss.getOrElse(stream.stats.accumulatedLoss))

    }

    val buyAndHoldChange: BigDecimal =
    if (firstPrice.getOrElse(stream.stats.firstPrice) * (signal.price - firstPrice.getOrElse(stream.stats.firstPrice)) == 0) {
      0
    } else {
      1 / firstPrice.getOrElse(stream.stats.firstPrice) * (signal.price - firstPrice.getOrElse(stream.stats.firstPrice))
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
      partProfitableTrades = partProfitableTrades,
      partLoosingTrades = partLoosingTrades,
      profitFactor = profitFactor,
      buyAndHoldChange = buyAndHoldChange,
      averageWinningTrade = averageWinningTrade,
      averageLoosingTrade = averageLoosingTrade,
      averageMonthlyProfitIncl = allTimeProfitInclFees,
      averageMonthlyProfitExcl = averageMonthlyProfitExcl,
      monthsOfTrading = streamDuration / monthMs,
      maxDrawDown = maxDrawDown,
      firstPrice = firstPrice.getOrElse(stream.stats.firstPrice),
      allTimeProfitExcl = allTimeProfitExcl,
      allTimeProfitIncl = allTimeProfitIncl
    )

    SStream(stream.id, stream.exchange, stream.currencyPair, stream.apiKey, signal.signal, adoptedStreamStats, cComponents)
  }

}
