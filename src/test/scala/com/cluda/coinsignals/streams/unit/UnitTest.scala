package com.cluda.coinsignals.streams.unit

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest._

/**
 * All unit tests should extend this.
 */
abstract class
UnitTest extends FlatSpecLike with Matchers with
OptionValues with Inside with Inspectors with BeforeAndAfterAll {

}