package com.cluda.coinsignals.streams.unit

import com.cluda.coinsignals.streams.TestData
import com.cluda.coinsignals.streams.util.StatsUtil

class StatsUtilTest extends UnitTest {

  "updateStreamWitheNewSignal" should
    "tak a 'SStream' and a new signal, and return a 'SStream' updated with the new signal" in {
    assert(TestData.freshStream.status == 0)

    val newSStream = StatsUtil.updateStreamWitheNewSignal(TestData.freshStream, TestData.signal1)
    assert(newSStream.status == 1)

    val newSStream2 = StatsUtil.updateStreamWitheNewSignal(newSStream, TestData.signal0)
    assert(newSStream2.status == 0)
    assert(newSStream2.stats.firstPrice == TestData.signal1.price)
    assert(newSStream2.stats.accumulatedProfit > 0)

    val newSStream3 = StatsUtil.updateStreamWitheNewSignal(newSStream2, TestData.signalminus1)
    assert(newSStream3.status == -1)

    val newSStream4 = StatsUtil.updateStreamWitheNewSignal(newSStream3, TestData.signal0)
    assert(newSStream4.status == 0)
    assert(newSStream4.stats.numberOfClosedTrades == 2)
    assert(newSStream4.stats.numberOfSignals == 4)
    assert(newSStream2.stats.firstPrice == TestData.signal1.price)

  }

}
