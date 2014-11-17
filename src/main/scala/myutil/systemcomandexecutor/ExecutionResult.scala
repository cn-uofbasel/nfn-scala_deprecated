package myutil.systemcomandexecutor

sealed trait ExecutionResult {
  def data: Array[Byte]
  def cmds: List[String]
}
case class ExecutionSuccess(cmds: List[String], data: Array[Byte]) extends ExecutionResult {
  override def toString = {
    new String(s"ExecutionSuccess(cmds: $cmds, res(s=${data.size}): ${new String(data)}")
  }
}
case class ExecutionError(cmds: List[String], data: Array[Byte], errCode: Int) extends ExecutionResult {
  override def toString = {
    new String(s"ExecutionError(cmds: $cmds, res(s=${data.size}): ${new String(data)}, errCode: $errCode")
  }
}
