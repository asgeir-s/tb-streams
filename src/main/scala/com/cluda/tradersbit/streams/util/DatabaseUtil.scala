package com.cluda.tradersbit.streams.util

import java.util.UUID

import awscala.dynamodbv2._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.services.sns.AmazonSNSClient
import com.cluda.tradersbit.streams.model._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future, Promise}

object DatabaseUtil {

  def awscalaDB(config: Config): DynamoDB = {
    implicit val region: Region = awscala.Region(config.getString("aws.dynamo.region"))
    val awsAccessKeyId = config.getString("aws.accessKeyId")
    val awsSecretAccessKey = config.getString("aws.secretAccessKey")

    if (awsAccessKeyId == "none" || awsSecretAccessKey == "none") {
      awscala.dynamodbv2.DynamoDB()
    }
    else {
      awscala.dynamodbv2.DynamoDB(awscala.BasicCredentialsProvider(awsAccessKeyId, awsSecretAccessKey))
    }
  }

  def removeStream(table: Table, streamID: String)(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[Boolean] = Future {
    table.deleteItem(streamID)
    println("DatabaseUtil: REMOVED stream with id: " + streamID)
    true
  }

  def updateSubscriptionPrice(table: Table, streamID: String, newSubscriptionPrice: BigDecimal)(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[Boolean] = Future {
    table.putAttributes(streamID, Seq(("subscriptionPriceUSD", newSubscriptionPrice)))
    true
  }

  def addSnsTopicArn(table: Table, streamID: String, topicArn: String)(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[Boolean] = Future {
    table.putAttributes(streamID, Seq("topicArn" -> topicArn))
    true
  }

  private def tableForcePut(table: Table, stream: SStream, lastSignal: Option[Signal])(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[SStream] = {
    val promise = Promise[SStream]()

    import spray.json._
    import SignalJsonProtocol._
    table.put(
      stream.id.get,
      "name" -> stream.name,
      "exchange" -> stream.exchange,
      "currencyPair" -> stream.currencyPair,
      "apiKeyId" -> stream.streamPrivate.apiKeyId,
      "topicArn" -> stream.streamPrivate.topicArn,
      "payoutAddress" -> stream.streamPrivate.payoutAddress,

      "idOfLastSignal" -> stream.idOfLastSignal,
      "subscriptionPriceUSD" -> stream.subscriptionPriceUSD,

      "status" -> stream.status,
      "timeOfFirstSignal" -> stream.stats.timeOfFirstSignal,
      "timeOfLastSignal" -> stream.stats.timeOfLastSignal,
      "numberOfSignals" -> stream.stats.numberOfSignals,
      "numberOfClosedTrades" -> stream.stats.numberOfClosedTrades,
      "numberOfProfitableTrades" -> stream.stats.numberOfProfitableTrades,
      "numberOfLoosingTrades" -> stream.stats.numberOfLoosingTrades,
      "accumulatedProfit" -> stream.stats.accumulatedProfit,
      "accumulatedLoss" -> stream.stats.accumulatedLoss,
      "averageTrade" -> stream.stats.averageTrade,
      "partWinningTrades" -> stream.stats.partWinningTrades,
      "partLoosingTrades" -> stream.stats.partLoosingTrades,
      "profitFactor" -> stream.stats.profitFactor,
      "buyAndHoldChange" -> stream.stats.buyAndHoldChange,
      "averageWinningTrade" -> stream.stats.averageWinningTrade,
      "averageLoosingTrade" -> stream.stats.averageLoosingTrade,
      "averageMonthlyProfitIncl" -> stream.stats.averageMonthlyProfitIncl,
      "averageMonthlyProfitExcl" -> stream.stats.averageMonthlyProfitExcl,
      "monthsOfTrading" -> stream.stats.monthsOfTrading,
      "maxDrawDown" -> stream.stats.maxDrawDown,
      "allTimeValueIncl" -> stream.stats.allTimeValueIncl,
      "allTimeValueExcl" -> stream.stats.allTimeValueExcl,
      "firstPrice" -> stream.stats.firstPrice,

      "maxDDPrevMax" -> stream.computeComponents.maxDDPrevMax,
      "maxDDPrevMin" -> stream.computeComponents.maxDDPrevMin,
      "maxDDMax" -> stream.computeComponents.maxDDMax,

      "lastSignal" -> {
        if (lastSignal.isDefined) {
          lastSignal.get.toJson.compactPrint
        }
        else {
          "{}".parseJson.compactPrint
        }
      }
    )

    promise.completeWith(getStreams(table, List(stream.id.get)).map(_.get.last))

    promise.future
  }

  def updateStream(table: Table, stream: SStream, lastSignal: Signal)(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[SStream] = {
    tableForcePut(table, stream, Some(lastSignal))
  }

  /**
    * Blocking!
    *
    * @param table streamsTable
    * @param stream id of the stream
    * @return Future[streamId]
    */
  def putStreamNew(table: Table, stream: SStream)(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[SStream] = {
    val promie = Promise[SStream]()
    val potensialId = UUID.randomUUID().toString

    // TODO: make put conditional so that it only succeed if id is not used
    table.get(potensialId) match {
      case None =>
        // id is available
        if (table.scan(Seq("name" -> cond.eq(stream.name))).isEmpty) {
          // name is available
          promie.completeWith(tableForcePut(table, stream.sStreamWithId(potensialId), None))
        }
        else {
          promie.failure(new Exception("name is already in use."))
        }
      case _ =>
        // id is used
        promie.failure(new Exception("the UUID potensialId assign is already in use."))
    }
    promie.future
  }


  def itemToStream(streamItem: Item): SStream = {
    import spray.json._
    import SignalJsonProtocol._
    val attrMap = streamItem.attributes.map(x => (x.name, x.value.s.getOrElse(x.value.n.getOrElse("")))).toMap

    val cComponents = ComputeComponents(
      maxDDPrevMax = BigDecimal(attrMap("maxDDPrevMax")),
      maxDDPrevMin = BigDecimal(attrMap("maxDDPrevMin")),
      maxDDMax = BigDecimal(attrMap("maxDDMax"))
    )

    val stats = StreamStats(
      timeOfFirstSignal = attrMap("timeOfFirstSignal").toLong,
      timeOfLastSignal = attrMap("timeOfLastSignal").toLong,
      numberOfSignals = attrMap("numberOfSignals").toLong,
      numberOfClosedTrades = attrMap("numberOfClosedTrades").toLong,
      numberOfProfitableTrades = attrMap("numberOfProfitableTrades").toLong,
      numberOfLoosingTrades = attrMap("numberOfLoosingTrades").toLong,
      accumulatedProfit = BigDecimal(attrMap("accumulatedProfit")),
      accumulatedLoss = BigDecimal(attrMap("accumulatedLoss")),
      averageTrade = BigDecimal(attrMap("averageTrade")),
      partWinningTrades = BigDecimal(attrMap("partWinningTrades")),
      partLoosingTrades = BigDecimal(attrMap("partLoosingTrades")),
      profitFactor = BigDecimal(attrMap("profitFactor")),
      buyAndHoldChange = BigDecimal(attrMap("buyAndHoldChange")),
      averageWinningTrade = BigDecimal(attrMap("averageWinningTrade")),
      averageLoosingTrade = BigDecimal(attrMap("averageLoosingTrade")),
      averageMonthlyProfitIncl = BigDecimal(attrMap("averageMonthlyProfitIncl")),
      averageMonthlyProfitExcl = BigDecimal(attrMap("averageMonthlyProfitExcl")),
      monthsOfTrading = BigDecimal(attrMap("monthsOfTrading")),
      maxDrawDown = BigDecimal(attrMap("maxDrawDown")),
      allTimeValueIncl = BigDecimal(attrMap("allTimeValueIncl")),
      allTimeValueExcl = BigDecimal(attrMap("allTimeValueExcl")),
      firstPrice = BigDecimal(attrMap("firstPrice"))
    )

    val sPrivate = StreamPrivate(
      apiKeyId = attrMap("apiKeyId"),
      topicArn = attrMap("topicArn"),
      payoutAddress = attrMap("payoutAddress")
    )

    val stream = SStream(
      id = Some(attrMap("id")),
      name = attrMap("name"),
      exchange = attrMap("exchange"),
      currencyPair = attrMap("currencyPair"),
      status = attrMap("status").toInt,
      idOfLastSignal = attrMap("idOfLastSignal").toLong,
      subscriptionPriceUSD = BigDecimal(attrMap("subscriptionPriceUSD")),
      stats = stats,
      computeComponents = cComponents,
      streamPrivate = sPrivate,
      lastSignal = {
        try {
          Some(attrMap("lastSignal").parseJson.convertTo[Signal])
        }
        catch {
          case e: Throwable => None
        }
      })

    stream
  }

  /**
    *
    * @param table streamsTable
    * @param streamIDs the streams corresponding to the id
    * @return
    */
  def getStreams(table: Table, streamIDs: List[String])(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[Option[List[SStream]]] = Future {
    val streamFromDb: Seq[Item] = {
      if (streamIDs.length == 1) {
        if (table.getItem(streamIDs.last).isDefined) {
          Seq(table.getItem(streamIDs.last).get)
        }
        else {
          Seq[Item]()
        }
      }
      else {
        table.batchGetItems(streamIDs.map(x => ("id", x)))
      }
    }

    if (streamFromDb.length > 0) {
      Some(streamFromDb.map(itemToStream).toList)
    }
    else {
      None
    }
  }

  def getAllStreams(table: Table)(implicit dynamoDB: DynamoDB, ec: ExecutionContext): Future[Seq[SStream]] = Future {
    table.scan(filter = Seq()).map(itemToStream)
  }
}
