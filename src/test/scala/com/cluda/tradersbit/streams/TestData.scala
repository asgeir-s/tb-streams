package com.cluda.tradersbit.streams

import com.cluda.tradersbit.streams.model._
import com.cluda.tradersbit.streams.model._

object TestData {
  val monthMs: Long = 2628000000l


  val timestamp = System.currentTimeMillis()
  val signal1 = Signal(1, 1, timestamp, BigDecimal(200), BigDecimal(0.002), BigDecimal(0), BigDecimal(0), BigDecimal(0))
  val signal0 = Signal(2, 0, timestamp, BigDecimal(400), BigDecimal(1), BigDecimal(1), BigDecimal(1-0.002), BigDecimal(0.998))
  val signalminus1 = Signal(3, -1, timestamp, BigDecimal(200), BigDecimal(-0.5), BigDecimal(0), BigDecimal(-0.502), BigDecimal(0))

  val freshStream = SStream(None, "freshStream","bitstamp", "btcUSD", 0, 0, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"))

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
    buyAndHoldChange = 0.30,
    maxDrawDown = 0.20
  )

  val adoptedStream = SStream(Some("Someid2"),"adoptedStream", "bitstamp", "btcUSD", 0, 3, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"), None, adoptedStreamStats, adoptedCC)


  val signalSeq = Seq(
    Signal(13, 1, System.currentTimeMillis(), BigDecimal(234.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(12, 0, System.currentTimeMillis() - 10000, BigDecimal(254.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(11, 1, System.currentTimeMillis() - 20000, BigDecimal(234.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(10, 0, System.currentTimeMillis() - 30000, BigDecimal(224.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(9, -1, System.currentTimeMillis() - 40000, BigDecimal(254.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(8, 0, System.currentTimeMillis() - 50000, BigDecimal(264.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(7, -1, System.currentTimeMillis() - 60000, BigDecimal(184.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(6, 0, System.currentTimeMillis() - 70000, BigDecimal(154.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(5, 1, System.currentTimeMillis() - 80000, BigDecimal(194.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(4, 0, System.currentTimeMillis() - 90000, BigDecimal(254.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(3, 1, System.currentTimeMillis() - 100000, BigDecimal(304.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(2, 0, System.currentTimeMillis() - 110000, BigDecimal(404.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8)),
    Signal(1, 0, System.currentTimeMillis() - 110000, BigDecimal(404.453), BigDecimal(0), BigDecimal(100), BigDecimal(-0.002), BigDecimal(99.8))
  )


  val signalSeqMath = Seq(
    Signal(6, 0, timestamp - monthMs*1, BigDecimal(975), BigDecimal(0.5), BigDecimal(1.5), BigDecimal(0.498), BigDecimal(1.5-0.002-0.002-0.002-0.002-0.002)),
    Signal(5, 1, timestamp - monthMs*2, BigDecimal(650), BigDecimal(0.002), BigDecimal(1), BigDecimal(0), BigDecimal(1-0.002-0.002-0.002-0.002)),
    Signal(4, 0, timestamp - monthMs*3, BigDecimal(450), BigDecimal(-0.5), BigDecimal(1), BigDecimal(-0.502), BigDecimal(1-0.002-0.002-0.002)),
    Signal(3, -1, timestamp - monthMs*4, BigDecimal(300), BigDecimal(0.002), BigDecimal(2), BigDecimal(0), BigDecimal(2-0.002-0.002)),
    Signal(2, 0, timestamp - monthMs*5, BigDecimal(200), BigDecimal(1), BigDecimal(2), BigDecimal(1-0.002), BigDecimal(2-0.002)),
    Signal(1, 1, timestamp - monthMs*6, BigDecimal(100), BigDecimal(0.002), BigDecimal(1), BigDecimal(0), BigDecimal(1))
  )

  val mathStream7 = SStream(Some("math-test"), "mathStream7", "bitstamp", "btcUSD", 0, 6, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*1,
      numberOfSignals = 6,
      numberOfClosedTrades = 3,
      numberOfProfitableTrades = 2,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 1.496,
      accumulatedLoss = 0.502,
      buyAndHoldChange = 8.75,
      maxDrawDown = 0.5025025025025025025025025025025025,
      firstPrice = 100,
      allTimeValueExcl = 1.5,
      allTimeValueIncl = 1.49
    ),
    ComputeComponents(
      maxDDPrevMax = 1.998,
      maxDDPrevMin = 0.994,
      maxDDMax = 1.998
    ))

  val mathStream6 = SStream(Some("math-test"), "mathStream6", "bitstamp", "btcUSD", 1, 5, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*2,
      numberOfSignals = 5,
      numberOfClosedTrades = 2,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 0.998,
      accumulatedLoss = 0.502,
      buyAndHoldChange = 3.5,
      maxDrawDown = 0.5025025025025025025025025025025025,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.994
    ),
    ComputeComponents(
      maxDDPrevMax = 1.998,
      maxDDPrevMin = 0.994,
      maxDDMax = 1.998
    ))


  val mathStream5 = SStream(Some("math-test"), "mathStream5", "bitstamp", "btcUSD", 0, 4, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*3,
      numberOfSignals = 4,
      numberOfClosedTrades = 2,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 1,
      accumulatedProfit = 0.998,
      accumulatedLoss = 0.502,
      buyAndHoldChange = 3.5,
      maxDrawDown = 0.5025025025025025025025025025025025,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 0.994
    ),
    ComputeComponents(
      maxDDPrevMax = 1.998,
      maxDDPrevMin = 0.994,
      maxDDMax = 1.998
    ))

  val mathStream4 = SStream(Some("math-test"), "mathStream4", "bitstamp", "btcUSD", -1, 3, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*4,
      numberOfSignals = 3,
      numberOfClosedTrades = 1,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 0.998,
      accumulatedLoss = 0,
      buyAndHoldChange = 1,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 2,
      allTimeValueIncl = 1.998
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 1.998
    ))


  val mathStream3 = SStream(Some("math-test"), "mathStream3", "bitstamp", "btcUSD", 0, 2, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*5,
      numberOfSignals = 2,
      numberOfClosedTrades = 1,
      numberOfProfitableTrades = 1,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 0.998,
      accumulatedLoss = 0,
      buyAndHoldChange = 1,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 2,
      allTimeValueIncl = 1.998
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 1.998
    ))


  val mathStream2 = SStream(Some("math-test"), "mathStream2", "bitstamp", "btcUSD", 1, 1, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,
    StreamStats(
      timeOfFirstSignal = timestamp - monthMs*6,
      timeOfLastSignal = timestamp - monthMs*6,
      numberOfSignals = 1,
      numberOfClosedTrades = 0,
      numberOfProfitableTrades = 0,
      numberOfLoosingTrades = 0,
      accumulatedProfit = 0,
      accumulatedLoss = 0,
      buyAndHoldChange = 0,
      maxDrawDown = 0,
      firstPrice = 100,
      allTimeValueExcl = 1,
      allTimeValueIncl = 1
    ),
    ComputeComponents(
      maxDDPrevMax = 1,
      maxDDPrevMin = 1,
      maxDDMax = 1
    ))

  val mathStream1 = SStream(Some("math-test"), "mathStream1",  "bitstamp", "btcUSD", 0, 0, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"))


  // fro actor test

  val mathStream7actor = SStream(Some("calculateStatsActorTestStream"), "mathStream7actor", "bitstamp", "btcUSD", 0, 6, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None, mathStream7.stats, mathStream7.computeComponents)

  val mathStream6actor = SStream(Some("calculateStatsActorTestStream"), "mathStream6actor", "bitstamp", "btcUSD", 1, 5, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None, mathStream6.stats, mathStream6.computeComponents)


  val mathStream5actor = SStream(Some("calculateStatsActorTestStream"), "mathStream5actor", "bitstamp", "btcUSD", 0, 4, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None, mathStream5.stats, mathStream5.computeComponents)

  val mathStream4actor = SStream(Some("calculateStatsActorTestStream"), "mathStream4actor", "bitstamp", "btcUSD", -1, 3, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None,mathStream4.stats, mathStream4.computeComponents)


  val mathStream3actor = SStream(Some("calculateStatsActorTestStream"), "mathStream3actor", "bitstamp", "btcUSD", 0, 2, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None, mathStream3.stats, mathStream3.computeComponents)


  val mathStream2actor = SStream(Some("calculateStatsActorTestStream"), "mathStream2actor", "bitstamp", "btcUSD", 1, 1, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"),
  None, mathStream2.stats, mathStream2.computeComponents)

  val mathStream1actor = SStream(Some("calculateStatsActorTestStream"), "mathStream1actor", "bitstamp", "btcUSD", 0, 0, 10, StreamPrivate("apiKey","topicARN", "btcAddress", "auth0|563c81e8ed40b21c52FAKE"))


  //Signal(id: Long, signal: Int, timestamp: Long, price: BigDecimal, change: BigDecimal, value: BigDecimal)


}
