package filterAccess.crypto

import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

import filterAccess.crypto.Helpers._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Encryption
 *
 *   Symmetric:        AES-256
 *   Public/Private:   RSA
 *
 */
object Encryption {


  /**
   * Symmetric Encryption with AES-256
   *
   * @param   data     Data to encrypt
   * @param   key      Symmetric Key (To fully utilize AES-256, "key" should at least take 256 bits even though variable key size in accepted)
   * @return           Encrypted Data (base64 encoded)
   */
  def symEncrypt(data: String, key: String): String = {

    // Prepare key
    //
    // Compute a SHA-256 hash of key:String to make sure that key used to perform
    // the actual encryption is of length 256 bit.
    val processingKey = computeHash(key.getBytes)

    // Convert data from String to Array[Byte]
    val byteData = data.getBytes

    // call symmetric encryption (AES-256)
    val result = symEncryptProcessing(byteData, processingKey)

    // Convert back to String
    byteToString(result)

  }


  /**
   * Asymmetric Encryption with RSA.
   *
   * @param   data         Data to encrypt
   * @param   pubKey       Public Key (base64 encoded)
   * @return               Encrypted Data (base64 encoded)
   */
  def pubEncrypt(data:String, pubKey: String): String = {

    // Restore public key
    val byteKey = stringToByte(pubKey)
    val pubKeySpec = new X509EncodedKeySpec(byteKey)
    val restoredKey = KeyFactory.getInstance("RSA").generatePublic(pubKeySpec)

    // initialize cypher
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, restoredKey)

    // do encryption
    val result = cipher.doFinal(data.getBytes);
    byteToString(result)

  }

}