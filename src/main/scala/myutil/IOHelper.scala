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

  def printToFile(f: File)(op: PrintWriter => Unit): Unit = {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: File, data: String): Unit = {
    printToFile(f)(_.println(data))
  }

  def writeToFile(f: File)(op: FileOutputStream => Unit): Unit = {
    val fos = new FileOutputStream(f)
    try { op(fos) } finally { fos.close() }
  }


  def writeToFile(f: File, data: Array[Byte]): Unit = {
    writeToFile(f) { _.write(data) }
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

  def inToOut(is: InputStream, os: OutputStream) = {
    Iterator.continually(is.read).takeWhile(_ != -1)
                                 .foreach(os.write)
  }
}
