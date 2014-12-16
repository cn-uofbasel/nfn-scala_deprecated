package lambdacalculus.machine.CallByName

import lambdacalculus.machine._

trait CBNMachineValue extends MachineValue
case class ClosureThunk(c: List[Instruction], e: List[MachineValue], maybeContextName: Option[String] = None) extends CBNMachineValue
