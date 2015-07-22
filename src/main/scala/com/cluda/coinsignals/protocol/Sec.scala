package com.cluda.coinsignals.protocol

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader

/**
 * Created by sogasg on 20/07/15.
 */
object Sec {

  def secureMessage(message: String): String = {
    val encryptedMessage = CryptUtil.generateSecureMessage(message)
    JwtUtil.create(encryptedMessage)
  }

  def validateAndDecryptMessage(message: String): Option[String] = {
    val encryptedMessageOpt = JwtUtil.validateAndRetrieve(message)
    if (encryptedMessageOpt.isDefined) {
      CryptUtil.receiveSecureMessage(encryptedMessageOpt.get)
    }
    else {
      None
    }
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
    val encryptedToken = CryptUtil.generateSecureToken(UUID.randomUUID().toString + "accepted" + UUID.randomUUID().toString)
    val jwtToken = JwtUtil.createToken(encryptedToken)
    RawHeader("x-groza-thow", jwtToken)
  }

  def autenticated(token: String): Boolean = {
    val encryptedToken = JwtUtil.validateAndRetrieveToken(token).get
    CryptUtil.validateToken(encryptedToken)
  }
}