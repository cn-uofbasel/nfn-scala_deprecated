package myutil.systemcomandexecutor

import java.io._
import com.typesafe.scalalogging.slf4j.Logging
import myutil.IOHelper
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Success


//object SystemCommandExecutor {
//  def apply(cmds: List[String], maybeInputData: Option[Array[Byte]] = None): SystemCommandExecutor = {
//    SystemCommandExecutor(List(cmds), maybeInputData)
//  }
//}
case class SystemCommandExecutor(cmdPipes: List[List[String]], maybeInputData: Option[Array[Byte]] = None) extends Logging {

  def futExecute()(implicit execContext: ExecutionContext): Future[ExecutionResult] = Future(execute())

  def execute(): ExecutionResult = {
    logger.debug(s"Executing: $this")
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

    val io = new ProcessIO(input, stdOut, stdErr)

    val procBuilder: ProcessBuilder =
      cmdPipes.map(Process(_))
              .reduceRight(_ #| _)
    val finalProc = procBuilder run io

    val exitCode = finalProc.exitValue()

    val execRes =
      if (exitCode == 0) {
        ExecutionSuccess(cmdPipes, resultOut.toByteArray)
      } else {
        val execErr = ExecutionError(cmdPipes, errorOut.toByteArray, exitCode)

        logger.error(s"Execution error: $execErr")
        execErr
      }
    finalProc.destroy()
    execRes
  }
}
