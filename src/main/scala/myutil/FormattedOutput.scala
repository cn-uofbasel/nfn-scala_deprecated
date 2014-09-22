package myutil

/**
 * Created by basil on 16/09/14.
 */
object FormattedOutput {

  def byteArrayToHex(bytes: Array[Byte]): String = bytes.map{ b => String.format("%02X", new java.lang.Integer(b & 0xff)) }.mkString("'", " ", "'")

}
