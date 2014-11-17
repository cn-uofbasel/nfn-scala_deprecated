package myutil.systemcomandexecutor

import java.io._
import com.typesafe.scalalogging.slf4j.Logging
import myutil.IOHelper
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

case class SystemCommandExecutor(cmds: List[String], maybeInputData: Option[Array[Byte]] = None) extends Logging {

  def execute()(implicit execContext: ExecutionContext): Future[ExecutionResult] = {
    logger.debug(s"Executing: $this")
    Future({
      val maybeInput =
        maybeInputData map { inputData =>
          new ByteArrayInputStream(inputData)
        }
      val resultOut = new ByteArrayOutputStream()
      val errorOut = new ByteArrayOutputStream()

      def input(os: java.io.OutputStream) = {
        maybeInput map { in =>
          IOHelper.inToOut(in, os)
        }
        os.close()
      }

      def stdOut(in: java.io.InputStream): Unit = {
        IOHelper.inToOut(in, resultOut)
        in.close()
      }

      def stdErr(errIn: java.io.InputStream): Unit = {
        IOHelper.inToOut(errIn, errorOut)
        errIn.close()
      }
      val proc = cmds run new ProcessIO(input, stdOut, stdErr)
      val exitCode = proc.exitValue()
      val execRes =
        if (exitCode == 0) {
          ExecutionSuccess(cmds, resultOut.toByteArray)
        } else {
          val execErr = ExecutionError(cmds, errorOut.toByteArray, exitCode)

          logger.error(s"Execution error: $execErr")
          execErr
        }
      proc.destroy()
      execRes
    })
  }
}
