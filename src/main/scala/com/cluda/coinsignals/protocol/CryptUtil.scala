package com.cluda.coinsignals.protocol

/**
 * Created by sogasg on 19/07/15.
 */

import java.security.AlgorithmParameters
import java.security.spec.KeySpec
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, Mac, SecretKey, SecretKeyFactory}

import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.{Base64, Hex}


object CryptUtil {

  val cipherString = "AES/CBC/PKCS5Padding"

  /**
   *
   * @param bytes data to be encrypted
   * @return the encrypted text and a IV (both is needed to decrypt the message)
   */
  private def encrypt(bytes: Array[Byte], salt: Array[Byte], password: Array[Char]): (String, String) = {
    val secretKey = getCryptKey(salt, password)
    val encipher = Cipher.getInstance(cipherString)
    encipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val params: AlgorithmParameters = encipher.getParameters()
    val iv: Array[Byte] = params.getParameterSpec(classOf[IvParameterSpec]).getIV()
    (Base64.encodeBase64String(encipher.doFinal(bytes)), Base64.encodeBase64String(iv))
  }

  /**
   *
   * @param bytes the encrypted data
   * @param iv the data's iv
   * @return the decrypted message
   */
  private def decrypt(bytes: Array[Byte], iv: String, salt: Array[Byte], password: Array[Char]): String = {
    val secretKey = getCryptKey(salt, password)
    val decipher = Cipher.getInstance(cipherString)

    decipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decodeBase64(iv)))
    new String(decipher.doFinal(Base64.decodeBase64(bytes)))
  }

  private def getCryptKey(salt: Array[Byte], password: Array[Char]): SecretKeySpec = {

    /* Derive the key, given password and salt. */
    val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec: KeySpec = new PBEKeySpec(password, salt, 65536, 256)
    val tmp: SecretKey = factory.generateSecret(spec)
    new SecretKeySpec(tmp.getEncoded(), "AES")
  }

  private def hmacEncode(data: String, secret: String) = {
    val sha256_HMAC: Mac = Mac.getInstance("HmacSHA256")
    val secret_key: SecretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256")
    sha256_HMAC.init(secret_key)
    Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")))
  }

  def generateSecureMessage(message: String): String = {
    val config = ConfigFactory.load()
    val salt = config.getString("crypt.salt").getBytes
    val password = config.getString("crypt.password").toCharArray
    val hKey = config.getString("crypt.hmac")

    val enc = encrypt(message.getBytes, salt, password)
    val encMessage = enc._1
    val encIv = enc._2

    val hmac = hmacEncode(encMessage, hKey)

    """{"hash":"""" + hmac + """","iv":"""" + encIv + """","data":"""" + encMessage + """"}"""
  }

  def receiveSecureMessage(message: String): Option[String] = {
    val config = ConfigFactory.load()
    val salt = config.getString("crypt.salt").getBytes
    val password = config.getString("crypt.password").toCharArray
    val hKey = config.getString("crypt.hmac")

    import spray.json._

    val json = message.parseJson

    val fields = json.asJsObject.getFields("hash", "data", "iv")
    val hash = fields(0).toString().drop(1).dropRight(1)
    val data = fields(1).toString().drop(1).dropRight(1)
    val iv = fields(2).toString().drop(1).dropRight(1)

    try {
      if (hmacEncode(data, hKey) == hash) {
        Some(decrypt(data.getBytes, iv, salt, password))
      }
      else {
        None
      }
    }
    catch {
      case e: Throwable =>
        None
    }
  }

}