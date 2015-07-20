package com.cluda.coinsignals.protocol

import com.typesafe.config.ConfigFactory
import org.jose4j.jwk._
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.{InvalidJwtException, JwtConsumerBuilder}

/**
 * Created by sogasg on 20/07/15.
 */
object JwtUtil {

  def create(str: String): String = {

    val webKey = key

    val data = """{"content": """ + str + """}"""

    // Create the Claims, which will be the content of the JWT
    val claims = JwtClaims.parse(data)
    claims.setIssuer("coinsignals.com");  // who creates the token and signs it
    claims.setAudience("Audience"); // to whom the token is intended to be sent
    claims.setExpirationTimeMinutesInTheFuture(5); // time when the token will expire (10 minutes from now)
    claims.setGeneratedJwtId(); // a unique identifier for the token
    claims.setIssuedAtToNow();  // when the token was issued/created (now)
    claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
    claims.setSubject("subject"); // the subject/principal is whom the token is about


    // A JWT is a JWS and/or a JWE with JSON claims as the payload.
    // In this example it is a JWS so we create a JsonWebSignature object.
    val jws = new JsonWebSignature()

    // The payload of the JWS is JSON content of the JWT Claims
    jws.setPayload(claims.toJson)

    // The JWT is signed using the private key
    jws.setKey(webKey.getKey)

    // Set the Key ID (kid) header because it's just the polite thing to do.
    // We only have one key in this example but a using a Key ID helps
    // facilitate a smooth key rollover process
    jws.setKeyIdHeaderValue(webKey.getKeyId)

    // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256)

    // Sign the JWS and produce the compact serialization or the complete JWT/JWS
    // representation, which is a string consisting of three dot ('.') separated
    // base64url-encoded parts in the form Header.Payload.Signature
    // If you wanted to encrypt it, you can simply set this jwt as the payload
    // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
    val jwt = jws.getCompactSerialization


    // Now you can do something with the JWT. Like send it to some other party
    // over the clouds and through the interwebs.
    jwt
  }

  def validateAndRetrieve(jwt: String): Option[String] = {
    val webKey = key

    // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
    // be used to validate and process the JWT.
    // The specific validation requirements for a JWT are context dependent, however,
    // it typically advisable to require a expiration time, a trusted issuer, and
    // and audience that identifies your system as the intended recipient.
    // If the JWT is encrypted too, you need only provide a decryption key or
    // decryption key resolver to the builder.
    val jwtConsumer = new JwtConsumerBuilder()
      .setRequireExpirationTime() // the JWT must have an expiration time
      .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
      .setRequireSubject() // the JWT must have a subject claim
      .setExpectedIssuer("coinsignals.com") // whom the JWT needs to have been issued by
      .setExpectedAudience("Audience") // to whom the JWT is intended for
      .setVerificationKey(webKey.getKey) // verify the signature with the public key
      .build(); // create the JwtConsumer instance

    try
    {
      import spray.json._

      //  Validate the JWT and process it to the Claims
      val jwtClaims = jwtConsumer.processToClaims(jwt)
      Some(jwtClaims.getRawJson.parseJson.asJsObject.getFields("content")(0).toString())
    }
    catch {
      case e: InvalidJwtException =>
        // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
        // Hopefully with meaningful explanations(s) about what went wrong.
        None

    }

  }

  private def key: JsonWebKey = {

    val config = ConfigFactory.load()
    val kty = config.getString("crypt.jwt.kty")
    val k = config.getString("crypt.jwt.k")
    val key = """{ "kty":"""" + kty + """", "k": """" + k + """"}"""

    JsonWebKey.Factory.newJwk(key)
  }


}


