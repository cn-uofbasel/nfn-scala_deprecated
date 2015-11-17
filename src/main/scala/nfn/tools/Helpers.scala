package nfn.tools

import java.util.Base64

/**
 * Created by blacksheeep on 17/11/15.
 */
object Helpers {

  /**
   * Convert a Array[Byte] to a String (base64 encoding)
   * Inverse function of stringToByte(...)
   *
   * NOTE: Output String has more characters than length of data:Byte[Array] because of base64 encoding.
   *
   * @param   data  Data to convert
   * @return        Same data as String (base64 encoding)
   */
  def byteToString(data: Array[Byte]): String =
    Base64.getEncoder.encodeToString(data) // note: this is new in java 8


  /**
   * Convert a String (base64 encoding) to a Array[Byte].
   * Inverse function of byteToString(...)
   *
   * NOTE: data:String has more characters than length of returned Byte[Array] because of base64 encoding.
   *
   * @param   data  Data to convert (base64 encoding)
   * @return        Same data as Array[Byte]
   */
  def stringToByte(data: String): Array[Byte] =
    Base64.getDecoder.decode(data) // note: this is new in java 8



}
