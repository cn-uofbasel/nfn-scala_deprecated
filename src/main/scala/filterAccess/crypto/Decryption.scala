package filterAccess.crypto

import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import nfn.tools.Helpers._
import filterAccess.crypto.Helpers._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Decryption
 *
 *   Symmetric:        AES-256
 *   Public/Private:   RSA
 *
 */
object Decryption {


  /**
   * Symmetric Decryption with AES-256
   *
   * @param   data         Data to decrypt (base64 encoded)
   * @param   key          Symmetric Key
   * @return               Decrypted Data as String (Base64 Encoding)
   */
  def symDecrypt(data: String, key: String): String = {

    // Prepare key
    //
    // Compute a SHA-256 hash of key:Int to make sure that key used to perform
    // the actual encryption is of length 256 bit.
    val processingKey = computeHash(key.getBytes)

    // Convert data from String to Array[Byte]
    val byteData = stringToByte(data)

    // call symmetric encryption (AES-256)
    val result = symDecryptProcessing(byteData, processingKey)

    // Convert back to String
    new String(result)

  }


  /**
   * Asymmetric Decryption with RSA.
   *
   * @param   data         Data to decrypt (base64 encoded)
   * @param   privateKey   Private Key (base64 encoded)
   * @return               Decrypted Data as String
   */
  def privateDecrypt(data:String, privateKey: String): String = {

    // Restore public key
    val byteKey = stringToByte(privateKey)
    val privKeySpec = new PKCS8EncodedKeySpec(byteKey)
    val restoredKey = KeyFactory.getInstance("RSA").generatePrivate(privKeySpec)

    // initialize cypher
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.DECRYPT_MODE, restoredKey)

    // do encryption
    val result = cipher.doFinal(stringToByte(data))
    new String(result)

  }

}
