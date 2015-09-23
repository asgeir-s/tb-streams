package com.cluda.coinsignals.streams

import com.cluda.coinsignals.streams.model._

object TestData {
  val monthMs: Long = 2628000000l


  val timestamp = System.currentTimeMillis()
  val signal1 = Signal(1, 1, timestamp, BigDecimal(200), BigDecimal(0), BigDecimal(0))
  val signal0 = Signal(2, 0, timestamp, BigDecimal(400), BigDecimal(1), BigDecimal(1))
  val signalminus1 = Signal(3, -1, timestamp, BigDecimal(200), BigDecimal(-0.5), BigDecimal(0))

  val freshStream = SStream(None, "bitstamp", "btcUSD", 0, 0, 10, StreamPrivate("apiKey","topicARN", "btcAddress"))

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
    partWinningTrades = 0.70,
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

  val adoptedStream = SStream(Some("Someid2"), "bitstamp", "btcUSD", 0, 3, 10, StreamPrivate("apiKey","topicARN", "btcAddress"), adoptedStreamStats, adoptedCC)


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


  val signalSeqMath = Seq(
    Signal(6, 0, timestamp - monthMs*1, BigDecimal(975), BigDecimal(0.5), BigDecimal(1.5)),
    Signal(5, 1, timestamp - monthMs*2, BigDecimal(650), BigDecimal(0), BigDecimal(1)),
    Signal(4, 0, timestamp - monthMs*3, BigDecimal(450), BigDecimal(-0.5), BigDecimal(1)),
    Signal(3, -1, timestamp - monthMs*4, BigDecimal(300), BigDecimal(0), BigDecimal(2)),
    Signal(2, 0, timestamp - monthMs*5, BigDecimal(200), BigDecimal(1), BigDecimal(2)),
    Signal(1, 1, timestamp - monthMs*6, BigDecimal(100), BigDecimal(0), BigDecimal(1))
  )

  val mathStream7 = SStream(Some("math-test"), "bitstamp", "btcUSD", 0, 6, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*1,
      numberOfSignals = 6,
      numberOfClosedTrades = 3,
      numberOfProfitableTrades = 2,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1.5,
      accumulatedLoss = 0.5,
      averageTrade = 0.3333333333333333333333333333333333,
      partWinningTrades = 0.6666666666666666666666666666666667,
      partLoosingTrades = 0.3333333333333333333333333333333333,
      profitFactor = 3,
      buyAndHoldChange = 8.75,
      averageWinningTrade = 0.75,
      averageLoosingTrade = 0.5,
      averageMonthlyProfitIncl = 0.09625,
      averageMonthlyProfitExcl = 0.1,
      monthsOfTrading = 5,
      maxDrawDown = 0.5,
      firstPrice = 100,
      allTimeValueExcl = 1.5,
      allTimeValueIncl = 1.48125
    ),
    ComputeComponents(
      maxDDPrevMax = 2,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))

  val mathStream6 = SStream(Some("math-test"), "bitstamp", "btcUSD", 1, 5, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*2,
      numberOfSignals = 5,
      numberOfClosedTrades = 2,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1,
      accumulatedLoss = 0.5,
      averageTrade = 0.25,
      partWinningTrades = 0.5,
      partLoosingTrades = 0.5,
      profitFactor = 2,
      buyAndHoldChange = 5.5,
      averageWinningTrade = 1,
      averageLoosingTrade = 0.5,
      averageMonthlyProfitIncl = -0.0025,
      averageMonthlyProfitExcl = 0,
      monthsOfTrading = 4,
      maxDrawDown = 0.5,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.99
    ),
    ComputeComponents(
      maxDDPrevMax = 2,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))


  val mathStream5 = SStream(Some("math-test"), "bitstamp", "btcUSD", 0, 4, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*3,
      numberOfSignals = 4,
      numberOfClosedTrades = 2,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1,
      accumulatedLoss = 0.5,
      averageTrade = 0.25,
      partWinningTrades = 0.5,
      partLoosingTrades = 0.5,
      profitFactor = 2,
      buyAndHoldChange = 3.5,
      averageWinningTrade = 1,
      averageLoosingTrade = 0.5,
      averageMonthlyProfitIncl = -0.0025,
      averageMonthlyProfitExcl = 0,
      monthsOfTrading = 3,
      maxDrawDown = 0.5,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.9925
    ),
    ComputeComponents(
      maxDDPrevMax = 2,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))

  val mathStream4 = SStream(Some("math-test"), "bitstamp", "btcUSD", -1, 3, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*4,
      numberOfSignals = 3,
      numberOfClosedTrades = 1,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 1,
      accumulatedLoss = 0,
      averageTrade = 1,
      partWinningTrades = 1,
      partLoosingTrades = 0,
      profitFactor = 0,
      buyAndHoldChange = 2,
      averageWinningTrade = 1,
      averageLoosingTrade = 0,
      averageMonthlyProfitIncl = 0.49375,
      averageMonthlyProfitExcl = 0.5,
      monthsOfTrading = 2,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 2,
      allTimeValueIncl = 1.9875
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))


  val mathStream3 = SStream(Some("math-test"), "bitstamp", "btcUSD", 0, 2, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*5,
      numberOfSignals = 2,
      numberOfClosedTrades = 1,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 1,
      accumulatedLoss = 0,
      averageTrade = 1,
      partWinningTrades = 1,
      partLoosingTrades = 0,
      profitFactor = 0,
      buyAndHoldChange = 1,
      averageWinningTrade = 1,
      averageLoosingTrade = 0,
      averageMonthlyProfitIncl = 0.99,
      averageMonthlyProfitExcl = 1,
      monthsOfTrading = 1,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 2,
      allTimeValueIncl = 1.99
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))


  val mathStream2 = SStream(Some("math-test"), "bitstamp", "btcUSD", 1, 1, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*6,
      numberOfSignals = 1,
      numberOfClosedTrades = 0,
      numberOfProfitableTrades = 0,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 0,
      accumulatedLoss = 0,
      averageTrade = 0,
      partWinningTrades = 0,
      partLoosingTrades = 0,
      profitFactor = 0,
      buyAndHoldChange = 0,
      averageWinningTrade = 0,
      averageLoosingTrade = 0,
      averageMonthlyProfitIncl = 0,
      averageMonthlyProfitExcl = 0,
      monthsOfTrading = 0,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.9975
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 1
    ))

  val mathStream1 = SStream(Some("math-test"), "bitstamp", "btcUSD", 0, 0, 10, StreamPrivate("apiKey","topicARN", "btcAddress"))




  // fro actor test

  val mathStream7actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", 0, 6, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*1,
      numberOfSignals = 6,
      numberOfClosedTrades = 3,
      numberOfProfitableTrades = 2,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1.5,
      accumulatedLoss = 0.5,
      averageTrade = 0.3333333333333333333333333333333333,
      partWinningTrades = 0.6666666666666666666666666666666667,
      partLoosingTrades = 0.3333333333333333333333333333333333,
      profitFactor = 3,
      buyAndHoldChange = 8.75,
      averageWinningTrade = 0.75,
      averageLoosingTrade = 0.5,
      averageMonthlyProfitIncl = 0.09625,
      averageMonthlyProfitExcl = 0.1,
      monthsOfTrading = 5,
      maxDrawDown = 0.5,
      firstPrice = 100,
      allTimeValueExcl = 1.5,
      allTimeValueIncl = 1.48125
    ),
    ComputeComponents(
      maxDDPrevMax = 2,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))

  val mathStream6actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", 1, 5, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*2,
      numberOfSignals = 5,
      numberOfClosedTrades = 2,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1,
      accumulatedLoss = 0.5,
      averageTrade = 0.25,
      partWinningTrades = 0.5,
      partLoosingTrades = 0.5,
      profitFactor = 2,
      buyAndHoldChange = 5.5,
      averageWinningTrade = 1,
      averageLoosingTrade = 0.5,
      averageMonthlyProfitIncl = -0.0025,
      averageMonthlyProfitExcl = 0,
      monthsOfTrading = 4,
      maxDrawDown = 0.5,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.99
    ),
    ComputeComponents(
      maxDDPrevMax = 2,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))


  val mathStream5actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", 0, 4, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*3,
      numberOfSignals = 4,
      numberOfClosedTrades = 2,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1,
      accumulatedLoss = 0.5,
      averageTrade = 0.25,
      partWinningTrades = 0.5,
      partLoosingTrades = 0.5,
      profitFactor = 2,
      buyAndHoldChange = 3.5,
      averageWinningTrade = 1,
      averageLoosingTrade = 0.5,
      averageMonthlyProfitIncl = -0.0025,
      averageMonthlyProfitExcl = 0,
      monthsOfTrading = 3,
      maxDrawDown = 0.5,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.9925
    ),
    ComputeComponents(
      maxDDPrevMax = 2,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))

  val mathStream4actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", -1, 3, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*4,
      numberOfSignals = 3,
      numberOfClosedTrades = 1,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 1,
      accumulatedLoss = 0,
      averageTrade = 1,
      partWinningTrades = 1,
      partLoosingTrades = 0,
      profitFactor = 0,
      buyAndHoldChange = 2,
      averageWinningTrade = 1,
      averageLoosingTrade = 0,
      averageMonthlyProfitIncl = 0.49375,
      averageMonthlyProfitExcl = 0.5,
      monthsOfTrading = 2,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 2,
      allTimeValueIncl = 1.9875
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))


  val mathStream3actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", 0, 2, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*5,
      numberOfSignals = 2,
      numberOfClosedTrades = 1,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 1,
      accumulatedLoss = 0,
      averageTrade = 1,
      partWinningTrades = 1,
      partLoosingTrades = 0,
      profitFactor = 0,
      buyAndHoldChange = 1,
      averageWinningTrade = 1,
      averageLoosingTrade = 0,
      averageMonthlyProfitIncl = 0.99,
      averageMonthlyProfitExcl = 1,
      monthsOfTrading = 1,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 2,
      allTimeValueIncl = 1.99
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 2
    ))


  val mathStream2actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", 1, 1, 10, StreamPrivate("apiKey","topicARN", "btcAddress"),
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*6,
      numberOfSignals = 1,
      numberOfClosedTrades = 0,
      numberOfProfitableTrades = 0,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 0,
      accumulatedLoss = 0,
      averageTrade = 0,
      partWinningTrades = 0,
      partLoosingTrades = 0,
      profitFactor = 0,
      buyAndHoldChange = 0,
      averageWinningTrade = 0,
      averageLoosingTrade = 0,
      averageMonthlyProfitIncl = 0,
      averageMonthlyProfitExcl = 0,
      monthsOfTrading = 0,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.9975
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 1
    ))

  val mathStream1actor = SStream(Some("calculateStatsActorTestStream"), "bitstamp", "btcUSD", 0, 0, 10, StreamPrivate("apiKey","topicARN", "btcAddress"))


  //Signal(id: Long, signal: Int, timestamp: Long, price: BigDecimal, change: BigDecimal, value: BigDecimal)


}
