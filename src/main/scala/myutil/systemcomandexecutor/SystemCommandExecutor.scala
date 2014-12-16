package myutil.systemcomandexecutor

import java.io._
import java.util.concurrent.TimeoutException
import com.typesafe.scalalogging.slf4j.Logging
import myutil.IOHelper
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.sys.process._
import scala.concurrent.duration._


case class SystemCommandExecutor(cmdPipes: List[List[String]], maybeInputData: Option[Array[Byte]] = None) extends Logging {

//  def executeEC(ec: ExecutionContext): ExecutionResult = {
//    execute()(ec)
//  }

  def futExecute()(implicit ec: ExecutionContext): Future[ExecutionResult] = {
    Future { execute() }
  }

  def executeWithTimeout(timeout: FiniteDuration = 5.seconds)(implicit ec: ExecutionContext): ExecutionResult = {
    try {
      val execRes = Await.result(
        Future {
          execute()
        },
        timeout
      )
      execRes
    } catch {
      case e: TimeoutException => ExecutionError(cmdPipes, Array(), None)
    }
  }

  // This call is blocking and depending on the exeucted result might never finish
  // use executeWithTimeout for a terminating version
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
    finalProc.destroy()
    if (exitCode == 0) {
      val execRes = ExecutionSuccess(cmdPipes, resultOut.toByteArray)
      logger.debug(s"Completed execution of $cmdPipes")
      execRes
    } else {
      val execErr = ExecutionError(cmdPipes, errorOut.toByteArray, Some(exitCode))

      logger.error(s"Execution error: $execErr")
      execErr
    }
  }
}
