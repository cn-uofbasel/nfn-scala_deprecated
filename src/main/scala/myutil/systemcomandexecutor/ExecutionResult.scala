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

//  override def |(pipedCmds: List[String]): Future[ExecutionResult] = {
//
//    val in = if(data.nonEmpty) Some(data) else None
//
//    SystemCommandExecutor(pipedCmds, in).execute() map {
//      case ExecutionSuccess(_, data) =>
//        val allCmds: List[String] = cmds ++ List("|") ++ pipedCmds
//        ExecutionSuccess(allCmds, data)
//      case ExecutionError(_, errData, errCode) =>
//        val allCmds: List[String] = cmds ++ List("|") ++ pipedCmds
//        ExecutionError(allCmds, errData, errCode)
//    }
//  }
}
case class ExecutionError(cmds: List[List[String]], data: Array[Byte], errCode: Int) extends ExecutionResult {
  override def toString = {
    new String(s"ExecutionError(cmds: $cmds, res(s=${data.size}): ${new String(data)}, errCode: $errCode")
  }

//  override def |(pipedCmds: List[String]): Future[ExecutionResult] = Future(
//    val allCmds: List[String] = cmds ++ List("|") ++ pipedCmds
//    ExecutionError(allCmds)
//  )
}
