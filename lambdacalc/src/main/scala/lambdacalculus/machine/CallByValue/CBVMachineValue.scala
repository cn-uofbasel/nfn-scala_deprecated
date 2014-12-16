package lambdacalculus.machine.CallByValue

import lambdacalculus.machine._


sealed trait CBVMachineValue extends MachineValue
case class ClosureMachineValue(varName: String, c: List[Instruction], e: List[MachineValue], maybeContextName: Option[String] = None) extends CBVMachineValue
case class EnvMachineValue(c: List[MachineValue], maybeContextName: Option[String] = None) extends CBVMachineValue

case class VariableMachineValue(n: String, maybeContextName: Option[String] = None) extends CBVMachineValue
//case class ListValue(values: Seq[Value], maybeContextName: Option[String] = None) extends CBVValue

