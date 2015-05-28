package com.cluda.coinsignals.streams.poststream

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import awscala.dynamodbv2._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.model.{DescribeTableRequest, TableStatus}
import com.amazonaws.services.sns.AmazonSNSClient
import com.cluda.coinsignals.streams.protocoll.NewStream
import com.cluda.coinsignals.streams.util.{AwsSnsUtil, DatabaseUtil}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

class PostStreamActor(tableName: String) extends Actor with ActorLogging {

  val config = ConfigFactory.load()
  implicit val region: Region = awscala.Region.US_WEST_2
  val awscalaCredentials = awscala.BasicCredentialsProvider(config.getString("aws.accessKeyId"), config.getString("aws.secretAccessKey"))
  val awsJavaCredentials = new BasicAWSCredentials(config.getString("aws.accessKeyId"), config.getString("aws.secretAccessKey"))


  implicit val dynamoDB = awscala.dynamodbv2.DynamoDB(awscalaCredentials)

  implicit val ec: ExecutionContext = context.system.dispatcher

  println(config.getString("aws.accessKeyId") + " - " + config.getString("aws.secretAccessKey"))


  import collection.JavaConversions._
  val topicSubscribers: List[String] = config.getStringList("awsTopicSubscribers").toList

  println("start finding streamsTable")

  private val streamsTable: awscala.dynamodbv2.Table = {
    if (dynamoDB.table(tableName).isEmpty) {
      println("dynamoDB.table(tableName).isEmpty")
      createAndWaitForTable(tableName)
    }
    else {
      println("found table")
      dynamoDB.table(tableName).get
    }
  }
  println("Finished finding streamsTable")


  def createAndWaitForTable(tableName: String): awscala.dynamodbv2.Table = {
    log.info("PostStreamActor: creating streamsTable with name " + tableName + " and witing for it to become ACTIVE")
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
    log.info("PostStreamActor: streamsTable with name " + tableName + " is ready.")
    dynamoDB.table(tableName).get
  }

  override def receive: Receive = {
    case newStream: NewStream =>
      val s = sender()
      log.info("PostStreamActor: got new stream: " + newStream)
      //Crete AWS SNS Topic
      val snsClient: AmazonSNSClient = new AmazonSNSClient(awsJavaCredentials)
      snsClient.setRegion(Region.getRegion(Regions.US_WEST_2))
      AwsSnsUtil.createTopic(snsClient, newStream.id).map { arn =>
        log.info("PostStreamActor: (aws sns) topic created with arn: " + arn)
        topicSubscribers.map(AwsSnsUtil.addSubscriber(snsClient, arn, _))

        DatabaseUtil.putNewStream(dynamoDB, streamsTable, newStream, arn)
        s ! HttpResponse(StatusCodes.Accepted, entity = """{"id":""" + newStream.id + "}")
        self ! PoisonPill
      }

  }
}

object PostStreamActor {
  def props(tableName: String): Props = Props(new PostStreamActor(tableName))
}