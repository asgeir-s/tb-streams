package com.cluda.admin

import com.amazonaws.regions.{Regions, Region}
import com.cluda.tradersbit.streams.util.AwsSnsUtil
import com.typesafe.config.ConfigFactory

/**
  * Created by sogasg on 05/02/16.
  */
object AddSubscriptionToAllStreamTopics {
  val region = "us-east-1"
  val lambdaToAdd = "arn:aws:lambda:us-east-1:525932482084:function:tb-backend-lambda-node-comp-notify-email:prod"

  def main(args: Array[String]): Unit = {
    val snsClient = AwsSnsUtil.amazonSNSClient(ConfigFactory.load())
    snsClient.setRegion(Region.getRegion(Regions.fromName(region)))
    val streamTopics = AwsSnsUtil.getAllTopics(snsClient).filterNot((topic) => {
      topic.getTopicArn.contains("ElasticBeanstalkNotifications") ||
        topic.getTopicArn.contains("dynamodb") ||
        topic.getTopicArn.contains("tb-payment-alerts")
    })

    println("number of topics: " + streamTopics.length)
    streamTopics.map((topic) => println(topic.getTopicArn))

    //streamTopics.map((topic) => snsClient.subscribe(topic.getTopicArn, "lambda", lambdaToAdd))

  }
}
