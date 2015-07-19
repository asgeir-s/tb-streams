package com.cluda.coinsignals.streams.unit

import com.cluda.coinsignals.streams.TestData
import com.cluda.coinsignals.streams.util.StreamUtil

class StreamUtilTest extends UnitTest {

  "updateStreamWitheNewSignal" should
    "tak a 'SStream' and a new signal, and return a 'SStream' updated with the new signal" in {
    assert(TestData.freshStream.status == 0)

    val newSStream = StreamUtil.updateStreamWitheNewSignal(TestData.freshStream, TestData.signal1)
    assert(newSStream.status == 1)

    val newSStream2 = StreamUtil.updateStreamWitheNewSignal(newSStream, TestData.signal0)
    assert(newSStream2.status == 0)
    assert(newSStream2.stats.firstPrice == TestData.signal1.price)
    assert(newSStream2.stats.accumulatedProfit > 0)

    val newSStream3 = StreamUtil.updateStreamWitheNewSignal(newSStream2, TestData.signalminus1)
    assert(newSStream3.status == -1)

    val newSStream4 = StreamUtil.updateStreamWitheNewSignal(newSStream3, TestData.signal0)
    assert(newSStream4.status == 0)
    assert(newSStream4.stats.numberOfClosedTrades == 2)
    assert(newSStream4.stats.numberOfSignals == 4)
    assert(newSStream2.stats.firstPrice == TestData.signal1.price)
  }

  "[math test] updateStreamWitheNewSignal" should
    "tak a 'SStream' and a new signal, and return a 'SStream' updated with the new signal" in {
    val newSStream1 = StreamUtil.updateStreamWitheNewSignal(TestData.mathStream1, TestData.signalSeqMath(5))
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(newSStream1, TestData.mathStream2))

    val newSStream2 = StreamUtil.updateStreamWitheNewSignal(newSStream1, TestData.signalSeqMath(4))
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(newSStream2, TestData.mathStream3))

    val newSStream3 = StreamUtil.updateStreamWitheNewSignal(newSStream2, TestData.signalSeqMath(3))
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(newSStream3, TestData.mathStream4))

    val newSStream4 = StreamUtil.updateStreamWitheNewSignal(newSStream3, TestData.signalSeqMath(2))
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(newSStream4, TestData.mathStream5))

    val newSStream5 = StreamUtil.updateStreamWitheNewSignal(newSStream4, TestData.signalSeqMath(1))
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(newSStream5, TestData.mathStream6))

    val newSStream6 = StreamUtil.updateStreamWitheNewSignal(newSStream5, TestData.signalSeqMath(0))
    assert(StreamUtil.checkRoundedEqualityExceptApiKeyAndID(newSStream6, TestData.mathStream7))

  }
}
