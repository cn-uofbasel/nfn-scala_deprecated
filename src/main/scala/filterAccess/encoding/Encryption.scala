package filterAccess.encoding

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Encryption (Symmetric and Pub/Private)
 *
 */
object Encryption {

  /**
   * Symmetric Encryption
   *
   * This implementation is for testing purposes only.
   * Later on, this function should implement a stronger encryption algorithm!
   *
   * @param   data     Data
   * @param   key      Symmetric Key
   * @return           Encrypted Data
   */
  def symEncrypt(data: String, key: Int): String = {

    (for (c <- data) yield (c + key).toChar).toString

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
