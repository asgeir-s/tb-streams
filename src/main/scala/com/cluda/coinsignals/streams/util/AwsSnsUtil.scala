package com.cluda.coinsignals.streams.util

import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.{CreateTopicRequest, CreateTopicResult, SubscribeRequest}

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

}
