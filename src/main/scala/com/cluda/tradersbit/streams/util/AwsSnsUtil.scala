package com.cluda.tradersbit.streams.util

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

object AwsSnsUtil {

  def createTopic(snsClient: AmazonSNSClient, streamID: String)(implicit ec: ExecutionContext) = Future[String] {
    val createTopicRequest: CreateTopicRequest = new CreateTopicRequest(streamID)
    val createTopicResult: CreateTopicResult = snsClient.createTopic(createTopicRequest)
    createTopicResult.getTopicArn
  }

  def addSubscriber(snsClient: AmazonSNSClient, topicArn: String, url: String)(implicit ec: ExecutionContext) = Future[String] {
    val subRequest: SubscribeRequest = new SubscribeRequest(topicArn, "http", url)
    snsClient.subscribe(subRequest)
    snsClient.getCachedResponseMetadata(subRequest).getRequestId
  }

  def amazonSNSClient(config: Config): AmazonSNSClient = {
    val awsAccessKeyId = config.getString("aws.accessKeyId")
    val awsSecretAccessKey = config.getString("aws.secretAccessKey")

    if (awsAccessKeyId == "none" || awsSecretAccessKey == "none") {
      new AmazonSNSClient()
    }
    else {
      new AmazonSNSClient(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey))
    }
  }

  def getAllTopics(snsClient: AmazonSNSClient) = {
    import scala.collection.JavaConversions._
    snsClient.listTopics().getTopics.toList
  }

}
