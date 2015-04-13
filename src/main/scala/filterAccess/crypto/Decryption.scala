package filterAccess.crypto

import filterAccess.crypto.Helpers._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Decryption (Symmetric and Pub/Private)
 *
 */
object Decryption {


  /**
   * Symmetric Decryption with AES-256
   *
   * @param   data     Data
   * @param   key      Symmetric Key (To fully utilize AES-256, "key" should at least take 256 bits or 32 bytes)
   * @return           Decrypted Data as String (Base64 Encoding)
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
   * Decryption with private key.
   *
   * This implementation is for testing purposes only.
   * Later on, this function should implement a stronger encryption algorithm!
   *
   * @param   data         Data
   * @param   privateKey   Private Key
   * @return               Decrypted Data
   */
  def privateDecrypt(data:String, privateKey: Int): String = {

    // TODO
    data

  }

}
