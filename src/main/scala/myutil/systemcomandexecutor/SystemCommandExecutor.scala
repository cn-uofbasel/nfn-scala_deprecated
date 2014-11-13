package myutil.systemcomandexecutor

import java.io._
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.{ExecutionContext, Future}

case class SystemCommandExecutor(cmds: List[String], inputData: Option[Array[Byte]] = None) extends Logging {

  def inToOut(is: InputStream, os: OutputStream) = {
    Iterator.continually(is.read)
      .takeWhile(_ != -1)
      .foreach(os.write)
  }

  def execute()(implicit execContext: ExecutionContext): Future[ExecutionResult] = {
    Future({
      val rt = Runtime.getRuntime
      logger.debug(s"Executing: ${cmds.mkString("'_'")}")
      val proc = rt.exec(cmds.toArray)

      // pipe both stdout and stderr of the process to a ByteArrayOutputStream
      // since this must be run until each stream is fully read we put it into a future
      val procIn = new BufferedInputStream(proc.getInputStream)
      val resultOut = new ByteArrayOutputStream()
      val futResult = Future(inToOut(procIn, resultOut))

      val procErrIn = new BufferedInputStream(proc.getErrorStream)
      val errorOut = new ByteArrayOutputStream()
      val futError = Future(inToOut(procErrIn, errorOut))

      // if this command executer has input data, pipe it into the process
      inputData map { inputData =>
        val procOut = new BufferedOutputStream(proc.getOutputStream)
        procOut.write(inputData)
        procOut.close()
      }

      // block until process has finished running
      proc.waitFor()

      // make sure that buth the stdout and stderr is completely consumed
      while (!(futResult.isCompleted && futError.isCompleted)) {
        Thread.sleep(1)
      }

      val result = resultOut.toByteArray
      val err = errorOut.toByteArray

      val execRes =
        if (proc.exitValue() == 0) {
          ExecutionSuccess(cmds, result)
        } else {
          ExecutionError(cmds, err, proc.exitValue())
        }
      proc.destroy()

      execRes
    })
  }
}
