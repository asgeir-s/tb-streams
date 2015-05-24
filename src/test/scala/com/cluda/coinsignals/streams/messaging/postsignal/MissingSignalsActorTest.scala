package com.cluda.coinsignals.streams.messaging.postsignal

import akka.testkit.TestActorRef
import com.cluda.coinsignals.streams.messaging.MessagingTest
import com.cluda.coinsignals.streams.postsignal.MissingSignalsActor

class MissingSignalsActorTest extends MessagingTest {


  import akka.testkit.EventFilter

  "when it receives a id it" should
    "send a HTTP-request to the signal-service requesting all new signals sins that id " in {
    val actor = TestActorRef(MissingSignalsActor.props("test-stream1"))

    EventFilter.info(pattern = "/streams/test-stream1/signals\\?fromId=10", occurrences = 1).intercept {
      actor ! 10L
    }
  }

}
