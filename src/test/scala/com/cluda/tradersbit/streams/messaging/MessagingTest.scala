package com.cluda.tradersbit.streams.messaging

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps
import akka.testkit.EventFilter

/**
 * All unit tests should extend this.
 */
abstract class MessagingTest extends TestKit(ActorSystem("test", ConfigFactory.parseString(
  """
    |  akka{
    |     test {
    |       # factor by which to scale timeouts during tests, e.g. to account for shared
    |       # build system load
    |       timefactor =  1.0
    |
    |       # duration of EventFilter.intercept waits after the block is finished until
    |       # all required messages are received
    |       filter-leeway = 15s
    |
    |       # duration to wait in expectMsg and friends outside of within() block
    |       # by default
    |       single-expect-default = 15s
    |
    |       # The timeout that is added as an implicit by DefaultTimeout trait
    |       default-timeout = 15s
    |
    |       calling-thread-dispatcher {
    |         type = akka.testkit.CallingThreadDispatcherConfigurator
    |       }
    |     }
    |
    |     loggers = ["akka.testkit.TestEventListener"]
    | }""".stripMargin
))) with FlatSpecLike with ImplicitSender with Matchers with
OptionValues with Inside with Inspectors with BeforeAndAfterAll {

  val config = ConfigFactory.load("application-test")

  implicit val timeout = Timeout(10 second)
  implicit val context = system.dispatcher

  def afterTest(): Unit ={}

  override protected def afterAll() {
    afterTest()
    TestKit.shutdownActorSystem(system)
    super.afterAll()
    system.shutdown()
  }

}
