package lambdacalculus.machine.CallByValue

import lambdacalculus.machine._

case class CBVConfiguration(stack: List[MachineValue], env:List[MachineValue], code:List[Instruction]) extends Configuration {
  override def toString:String = {
    val sb = new StringBuilder()
    sb ++=   "stack: " ++ stack.mkString("(", ", ", ")")
    sb ++= "\nenv  : " ++ env.mkString("(", ", ", ")")
    sb ++= "\ncode : " ++ code.mkString("(", ", ", ")")
    sb.toString()
  }

  override def isTransformable:Boolean = !code.isEmpty
}
