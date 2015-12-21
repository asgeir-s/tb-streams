package com.cluda.tradersbit.streams.poststream

import java.util.UUID

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala.dynamodbv2._
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.model.{DescribeTableRequest, TableStatus}
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.SubscribeRequest
import com.cluda.tradersbit.streams.model.StreamPrivate
import com.cluda.tradersbit.streams.util.AwsSnsUtil
import com.cluda.tradersbit.streams.model.{StreamPrivate, SStream}
import com.cluda.tradersbit.streams.protocoll.NewStream
import com.cluda.tradersbit.streams.util.{AwsSnsUtil, DatabaseUtil}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

class PostStreamActor(globalRequestID: String, tableName: String) extends Actor with ActorLogging {

  val config = ConfigFactory.load()

  implicit val dynamoDB = DatabaseUtil.awscalaDB(ConfigFactory.load())
  implicit val ec: ExecutionContext = context.system.dispatcher

  import collection.JavaConversions._

  val topicSubscribersRaw: List[String] = config.getStringList("snsSubscribers").toList

  private val streamsTable: awscala.dynamodbv2.Table = {
    if (dynamoDB.table(tableName).isEmpty) {
      createAndWaitForTable(globalRequestID, tableName)
    }
    else {
      dynamoDB.table(tableName).get
    }
  }


  def createAndWaitForTable(globalRequestID: String, tableName: String): awscala.dynamodbv2.Table = {
    log.info(s"[$globalRequestID]: Creating streamsTable with name " + tableName +
      " and witing for it to become ACTIVE")

    dynamoDB.createTable(
      name = tableName,
      hashPK = "id" -> AttributeType.String
    )
    val startTime = System.currentTimeMillis()
    val endTime = startTime + (10 * 60 * 1000)
    var tableReady = false
    while (System.currentTimeMillis() < endTime && !tableReady) {
      Thread.sleep(1000 * 10)

      val request = new DescribeTableRequest().withTableName(tableName)
      val table = dynamoDB.describeTable(request).getTable
      if (table == null) {}
      else {
        val tableStatus = table.getTableStatus
        log.info("PostStreamActor: " + tableName + "-tabe  - current state: " + tableStatus)
        if (tableStatus.equals(TableStatus.ACTIVE.toString)) {
          tableReady = true
        }
      }

    }
    log.info(s"[$globalRequestID]: StreamsTable with name " + tableName + " is ready.")
    dynamoDB.table(tableName).get
  }

  override def receive: Receive = {
    case newStream: NewStream =>
      val s = sender()

      val newSStream = SStream(None, newStream.name, newStream.exchange, newStream.currencyPair, 0, 0, newStream.subscriptionPriceUSD,
        StreamPrivate("none", "none", newStream.payoutAddress), lastSignal = None)

      // add stream to the database and get id
      DatabaseUtil.putStreamNew(streamsTable, newSStream).map {
        case stream: SStream =>
          val streamId = stream.id.get
          val subscribers = topicSubscribersRaw.map(_.replace("'streamID'", streamId))

          // create topic from id
          val snsClient: AmazonSNSClient = AwsSnsUtil.amazonSNSClient(ConfigFactory.load())
          snsClient.setRegion(Region.getRegion(Regions.US_WEST_2))
          AwsSnsUtil.createTopic(snsClient, streamId).map { arn =>
            val result = snsClient.subscribe(arn, "lambda", config.getString("aws.lambda.notify.email"))
            if(result.getSubscriptionArn.length > 1) {
              log.info(s"[$globalRequestID] : emailNotifyLambda successfully subscribes to this new stream. StreamID: $streamId")
            }
            else {
              log.error(s"[$globalRequestID]: FATAL: emailNotifyLambda FAILED to subscribe to this new stream. StreamID: $streamId")
            }

            log.info(s"[$globalRequestID]:  (aws sns) topic created with arn: " + arn)
            subscribers.map(AwsSnsUtil.addSubscriber(snsClient, arn, _))
            DatabaseUtil.addSnsTopicArn(streamsTable, streamId, arn).map { un =>
              import spray.json._
              import DefaultJsonProtocol._
              s ! HttpResponse(StatusCodes.Accepted, entity = Map("id" -> streamId).toJson.prettyPrint)
            }.recover {
              case e: Throwable =>
                log.error(s"[$globalRequestID]: 'DatabaseUtil.addSnsTopicArn()' failed. Error: " + e.toString)
                s ! HttpResponse(StatusCodes.InternalServerError)
            }.andThen{
              case _ => self ! PoisonPill
            }
          }.recover {
            case e: Throwable =>
              log.error(s"[$globalRequestID]: 'AwsSnsUtil.createTopic()' failed. Error: " + e.toString)
              s ! HttpResponse(StatusCodes.InternalServerError)
              self ! PoisonPill
          }
      }.recover {
        case e: Throwable =>
          log.error(s"[$globalRequestID]: 'DatabaseUtil.putStreamNew()' failed. Error: " + e.toString)
          s ! HttpResponse(StatusCodes.InternalServerError)
          self ! PoisonPill
      }

    case ChangeSubscriptionPrice(streamID: String, newPrice: BigDecimal) =>
      val s = sender()

      DatabaseUtil.updateSubscriptionPrice(streamsTable, streamID, newPrice)
      s ! HttpResponse(StatusCodes.Accepted)
      self ! PoisonPill
  }
}

object PostStreamActor {
  def props(globalRequestID: String, tableName: String): Props = Props(new PostStreamActor(globalRequestID, tableName))
}

case class ChangeSubscriptionPrice(streamID: String, newPrice: BigDecimal)