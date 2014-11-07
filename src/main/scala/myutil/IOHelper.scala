package myutil

import java.io._

/**
 * Created by basil on 08/04/14.
 */
object IOHelper {

  def exceptionToString(error: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    error.printStackTrace(pw)
    sw.toString
  }

  /**
   * Usage:
   * import java.io._
   * val data = Array("Five","strings","in","a","file!")
   * printToFile(new File("example.txt"))(p => {
   *   data.foreach(p.println)
   * })
   * @param f
   * @param op
   * @return
   */
  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit): Unit = {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: java.io.File, data: String): Unit = {
    printToFile(f)(_.println(data))
  }

  def readByteArrayFromFile(fileName: String): Array[Byte] = {
    readByteArrayFromFile(new File(fileName))
  }
  def readByteArrayFromFile(file: File): Array[Byte] = {
    val bis = new BufferedInputStream(new FileInputStream(file))
    try {
      Stream.continually(bis.read).takeWhile(_ != -1 ).map(_.toByte).toArray
    } finally {
      bis.close()
    }
  }
}
