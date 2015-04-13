package filterAccess.crypto

import java.security.MessageDigest
import java.util.Base64 // note: this is new in java 8
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

import java.security.KeyPairGenerator



/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Helpers doing the actual work for encryption and decryption.
 *
 */
object Helpers {


  /**
   * Convert a Array[Byte] to a String.
   * Inverse function of stringToByte(...)
   *
   * @param   data  Data in from of Array[Byte]
   * @return        Same data as String
   */
  def byteToString(data: Array[Byte]): String =
    Base64.getEncoder.encodeToString(data) // note: this is new in java 8


  /**
   * Convert a String to a Array[Byte].
   * Inverse function of byteToString(...)
   *
   * @param   data  Data in from of String
   * @return        Same data as Array[Byte]
   */
  def stringToByte(data: String): Array[Byte] =
    Base64.getDecoder.decode(data) // note: this is new in java 8



  /**
   * Compute a SHA-256 hash of a given Array[Byte].
   * The length of the returned hash is 256 bit or 32 byte respectively.
   *
   * This function is intended to produce keys for the symmetric encryption (AES-256) with proper length.
   *
   * @param   s  Data to calculate a hash
   * @return     SHA-256 Hash
   */
  def computeHash(s: Array[Byte]): Array[Byte] = {

    // initialize digester
    val digester = MessageDigest.getInstance("SHA-256")
    digester.update(s)

    // compute SHA-256 hash
    digester.digest

  }

  /**
   * Compute a SHA-256 hash of a given String.
   * The length of the returned hash is 256 bit or 32 byte respectively.
   *
   * This function is intended to produce keys for the symmetric encryption (AES-256) with proper length.
   *
   * @param   s  Data to calculate a hash
   * @return     SHA-256 Hash as Base64 encoded String
   */
  def computeHash(s:String):String = byteToString(computeHash(s.getBytes))


  /**
   * Do the actual symmetric encryption with AES-256 (AES/CBC/PKCS5Padding).
   *
   * @param   data    Byte Array to encrypt
   * @param   key     Determines the actual encryption key
   * @return          Result of encryption
   */
  def symEncryptProcessing(data: Array[Byte], key: Array[Byte]): Array[Byte] = {

    // initialize encryption key
    val encryptionKey = new SecretKeySpec(key, "AES")

    // initialize cypher
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val initVec = computeHash("asfasfsafsaf".getBytes).drop(16)
    val initVecSpec = new IvParameterSpec(initVec)
    cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, initVecSpec)

    // do encryption
    cipher.doFinal(data)

  }


  /**
   * Do the actual symmetric decryption with AES-256 (AES/CBC/PKCS5Padding).
   *
   * @param   data    Byte Array to decrypt
   * @param   key     Determines the actual decryption key
   * @return          Result of decryption
   */
  def symDecryptProcessing(data: Array[Byte], key: Array[Byte]): Array[Byte] = {

    // initialize encryption key
    val decryptionKey = new SecretKeySpec(key, "AES")

    // initialize cypher
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    import javax.crypto.spec.IvParameterSpec
    val initVec = computeHash("asfasfsafsaf".getBytes).drop(16)
    val initVecSpec = new IvParameterSpec(initVec)
    cipher.init(Cipher.DECRYPT_MODE, decryptionKey, initVecSpec)

    // do encryption
    cipher.doFinal(data)
  }

  /**
   * Generates a random public-private key pair for RSA (1024 bit)
   *
   * Note: This function is not deterministic.
   *
   * NOTE: BECAUSE DEVELOPMENT AND DEBUGGING WITH SHORTER KEYS IS MORE CONVENIENT,
   *       THE ALGORITHM IS CHANGED TO RSA 512 BIT!
   *
   *       !!! CHANGE THIS ON PRODUCTION MODE !!!
   *
   * @return (public key, private key) - Encoded as base64.
   */
  def asymKeyGenerator(): (String, String) = {

    // initialize RSA 1024 bit key pair generator
    val generator = KeyPairGenerator.getInstance("RSA")
    //generator.initialize(1024)
    generator.initialize(512) // TODO change to 1024 on production mode...

    // generate key
    val keyPair = generator.generateKeyPair()

    // return public and private key as base64
    val publicKey = byteToString(keyPair.getPublic.getEncoded)
    val privateKey = byteToString(keyPair.getPrivate.getEncoded)
    (publicKey, privateKey)

  }

}
