package com.cluda.coinsignals.streams

import com.cluda.coinsignals.streams.model._
import com.cluda.coinsignals.streams.protocoll.NewStream

object TestData {
  val timestamp = System.currentTimeMillis()
  val signal1 = Signal(1, 1, timestamp, BigDecimal(200), BigDecimal(0), BigDecimal(0))
  val signal0 = Signal(2, 0, timestamp, BigDecimal(400), BigDecimal(1), BigDecimal(1))
  val signalminus1 = Signal(3, -1, timestamp, BigDecimal(200), BigDecimal(-0.5), BigDecimal(0))

  val freshStream = SStream("someid1", "bitstamp", "btcUSD", "secretkey", 0)

  val adoptedCC = ComputeComponents(
    maxDDPrevMax = 0.23,
    maxDDPrevMin = 0.35,
    maxDDMax = 0.35
  )

  val adoptedStreamStats = StreamStats(
    timeOfFirstSignal = 100000,
    timeOfLastSignal = 293736281,
    numberOfSignals = 140,
    numberOfClosedTrades = 139,
    numberOfProfitableTrades = 100,
    numberOfLoosingTrades = 39,
    accumulatedProfit = 1000.28376,
    accumulatedLoss = 700.01,
    averageTrade = 0.01,
    partProfitableTrades = 0.70,
    partLoosingTrades = 0.20,
    profitFactor = 2.54,
    buyAndHoldChange = 0.30,
    averageWinningTrade = 0.036,
    averageLoosingTrade = -0.007,
    averageMonthlyProfitIncl = 0.40,
    averageMonthlyProfitExcl = 0.60,
    monthsOfTrading = 13,
    maxDrawDown = 0.20
  )

  val adoptedStream = SStream("someid2", "bitstamp", "btcUSD", "secretkey", 0, adoptedStreamStats, adoptedCC)



  val signalSeq = Seq(
    Signal(13, 1, System.currentTimeMillis(), BigDecimal(234.453), BigDecimal(0), BigDecimal(100)),
    Signal(12, 0, System.currentTimeMillis() - 10000, BigDecimal(254.453), BigDecimal(0), BigDecimal(100)),
    Signal(11, 1, System.currentTimeMillis() - 20000, BigDecimal(234.453), BigDecimal(0), BigDecimal(100)),
    Signal(10, 0, System.currentTimeMillis() - 30000, BigDecimal(224.453), BigDecimal(0), BigDecimal(100)),
    Signal(9, -1, System.currentTimeMillis() - 40000, BigDecimal(254.453), BigDecimal(0), BigDecimal(100)),
    Signal(8, 0, System.currentTimeMillis() - 50000, BigDecimal(264.453), BigDecimal(0), BigDecimal(100)),
    Signal(7, -1, System.currentTimeMillis() - 60000, BigDecimal(184.453), BigDecimal(0), BigDecimal(100)),
    Signal(6, 0, System.currentTimeMillis() - 70000, BigDecimal(154.453), BigDecimal(0), BigDecimal(100)),
    Signal(5, 1, System.currentTimeMillis() - 80000, BigDecimal(194.453), BigDecimal(0), BigDecimal(100)),
    Signal(4, 0, System.currentTimeMillis() - 90000, BigDecimal(254.453), BigDecimal(0), BigDecimal(100)),
    Signal(3, 1, System.currentTimeMillis() - 100000, BigDecimal(304.453), BigDecimal(0), BigDecimal(100)),
    Signal(2, 0, System.currentTimeMillis() - 110000, BigDecimal(404.453), BigDecimal(0), BigDecimal(100)),
    Signal(1, 0, System.currentTimeMillis() - 110000, BigDecimal(404.453), BigDecimal(0), BigDecimal(100))
  )


}
