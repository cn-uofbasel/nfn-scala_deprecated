package myutil.systemcomandexecutor

import scala.concurrent.Future

sealed trait ExecutionResult {
  def data: Array[Byte]
  def cmds: List[List[String]]

//  def |(pipedCmds: List[String]): Future[ExecutionResult]

}
case class ExecutionSuccess(cmds: List[List[String]], data: Array[Byte]) extends ExecutionResult {
  override def toString = {
    new String(s"ExecutionSuccess(cmds: $cmds, res(s=${data.size}): ${new String(data)}")
  }
}



case class ExecutionError(cmds: List[List[String]], data: Array[Byte], errCode: Option[Int]) extends ExecutionResult {
  override def toString = {
    val e =
      errCode map {ec =>
        s"errCode: $errCode"
      } getOrElse { "internal error or timeout" }

    new String(s"ExecutionError(cmds: $cmds, res(s=${data.size}): ${new String(data)}, $e")
  }
}
