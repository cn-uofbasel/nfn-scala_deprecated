package filterAccess.encoding

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Decryption (Symmetric and Pub/Private)
 *
 */
object Decryption {

  /**
   * Symmetric Decryption
   *
   * This implementation is for testing purposes only.
   * Later on, this function should implement a stronger decryption algorithm!
   *
   * @param   data     Data
   * @param   key      Symmetric Key
   * @return           Decrypted Data
   */
  def symDecrypt(data: String, key: Int): String = {

    (for (c <- data) yield (c - key).toChar).toString

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
