package com.cluda.logger

import java.text.SimpleDateFormat
import java.util.Date

import akka.event.slf4j.Slf4jLogger

/**
 * Created by sogasg on 08/10/15.
 */
class CustomLogger  extends Slf4jLogger {

  override def formatTimestamp(timestamp: Long): String = {
    val d = new Date(timestamp)
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS Z").format(d)
   }

}