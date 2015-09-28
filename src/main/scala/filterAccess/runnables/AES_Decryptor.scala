
package filterAccess.runnables

import filterAccess.crypto.Encryption.symEncrypt
import filterAccess.crypto.Decryption.symDecrypt
import filterAccess.crypto.Helpers._

import scala.sys.process._



object AES_Decryptor{

  def getKey(pkey : String) : String = {
    val command = "sh scripts/requestP.sh " + pkey
    command !! match {
      case symkey: String => {
        symkey
      }
      case _ => ???
    }
  }

  def getEcho(text: String, pkey: String) : String = {
    val command = "sh scripts/echo.sh " + text + " " + pkey
    command !! match {
      case echo: String => {
        echo
      }
      case _ => ???
    }
  }

  def main (args: Array[String]) {

    val data = args(0)
    val pkey = args(1)

    println("\n")

    val symKey = getKey(pkey)
    println("Decryption Key: " + symKey)

    val echo = getEcho(data, pkey)
    println("Echo Reply: " + echo)

    println("Decrpyted: " + symDecrypt(echo.substring(0, echo.length-1), symKey.substring(0, symKey.length-1)))
  }

}
