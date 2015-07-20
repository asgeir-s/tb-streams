package com.cluda.coinsignals.protocol

import akka.http.scaladsl.model.{HttpResponse, StatusCode}

/**
 * Created by sogasg on 20/07/15.
 */
object SendReceiveHelper {

  def secureMessage(message: String): String = {
    val encryptedMessage = CryptUtil.generateSecureMessage(message)
    JwtUtil.create(encryptedMessage)
  }

  def vaidateAndReceiveMessage(message: String): Option[String] = {
    val encryptedMessage = JwtUtil.validateAndRetrieve(message).get
    CryptUtil.receiveSecureMessage(encryptedMessage)
  }

  def SecureHttpResponse(statusCode: StatusCode, entity: String): HttpResponse = {
    HttpResponse(statusCode, entity = secureMessage(entity))
  }
}