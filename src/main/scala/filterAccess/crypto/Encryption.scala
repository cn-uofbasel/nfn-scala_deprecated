package filterAccess.crypto

import filterAccess.crypto.Helpers._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Encryption
 *
 *   Symmetric:  AES-256
 *   Pub/Priv:   TODO
 *
 */
object Encryption {

  /**
   * Symmetric Encryption with AES-256
   *
   * @param   data     Data
   * @param   key      Symmetric Key (To fully utilize AES-256, the "key" should at least take 256 bits or 32 bytes)
   * @return           Encrypted Data as String (Base64 Encoding)
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
   * Encryption with public key.
   *
   * This implementation is for testing purposes only.
   * Later on, this function should implement a stronger encryption algorithm!
   *
   * @param   data         Data
   * @param   pubKey       Public Key
   * @return               Encrypted Data
   */
  def pubEncrypt(data:String, pubKey: Int): String = {

    // TODO
    data

  }

}