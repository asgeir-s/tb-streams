package com.cluda.coinsignals.protocol

import java.security.SecureRandom
import java.util.UUID

import akka.http.javadsl.server.values.Headers
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.{RawHeader, Authorization}
import akka.http.scaladsl.model._

/**
 * Created by sogasg on 20/07/15.
 */
object Sec {

  def secureMessage(message: String): String = {
    val encryptedMessage = CryptUtil.generateSecureMessage(message)
    JwtUtil.create(encryptedMessage)
  }

  def validateAndDecryptMessage(message: String): Option[String] = {
    val encryptedMessage = JwtUtil.validateAndRetrieve(message).get
    CryptUtil.receiveSecureMessage(encryptedMessage)
  }

  def secureHttpResponse(statusCode: StatusCode, entity: String): HttpResponse = {
    HttpResponse(statusCode, entity = secureMessage(entity)).addHeader(headerToken)
  }

  def secureHttpResponse(statusCode: StatusCode): HttpResponse = {
    HttpResponse(statusCode).addHeader(headerToken)
  }

  def secureHttpRequest(statusCode: HttpMethod, uri: String): HttpRequest = {
    HttpRequest(statusCode, uri = uri).addHeader(headerToken)
  }

  def headerToken: RawHeader = {
    RawHeader("x-groza-thow", secureMessage(UUID.randomUUID().toString + "accepted" + UUID.randomUUID().toString))
  }

  def authRequest(authHeaderValue: String): Boolean = {
      val decryptedOpt = validateAndDecryptMessage(authHeaderValue)
      if (decryptedOpt.isDefined) {
        if (decryptedOpt.get.contains("accepted")) {
          true
        }
        else {
          false
        }
      }
      else {
        false
      }
    }
}