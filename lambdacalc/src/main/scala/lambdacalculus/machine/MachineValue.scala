package lambdacalculus.machine

import lambdacalculus.machine.CallByValue._
import lambdacalculus.machine.CallByName._
import lambdacalculus.compiler._
import lambdacalculus.parser.ast.LambdaPrettyPrinter


/**
 * Machine Values are the elements which build up a [[Configuration]], either elements on the stack or in environments.
 */
trait MachineValue {
  def maybeContextName: Option[String]
  override def toString: String = ValuePrettyPrinter.apply(this, None)
}

case class ConstMachineValue(n: Int, maybeContextName: Option[String] = None) extends MachineValue
case class CodeMachineValue(c: List[Instruction], maybeContextName:Option[String] = None) extends MachineValue
case class ListMachineValue(values:Seq[MachineValue], maybeContextName: Option[String] = None) extends MachineValue
case class NopMachineValue() extends MachineValue{
  override def maybeContextName: Option[String] = None
}

object ValuePrettyPrinter {
  def apply(value: MachineValue, maybeCompiler: Option[Compiler] = None): String = {
    def instructionsToString(instr: List[Instruction]): String = maybeCompiler match {
        case Some(comp) => LambdaPrettyPrinter(comp.decompile(instr))
        case None => instr.mkString(",")
    }

    def throwNotImplementedError = throw new NotImplementedError(s"ValuePrettyPrinter has no conversion for value of type ${value.getClass.getCanonicalName}")

    value match {
      case cbnValue: CBNMachineValue => cbnValue match {
        case ClosureThunk(c, e, _) =>  {
          s"ClosThunk{ c: ${instructionsToString(c)} | e: $e }"
        }
        case _ => throwNotImplementedError
      }
      case cbvValue: CBVMachineValue => cbvValue match {
        case ClosureMachineValue(n, c, e, _) => s"ClosVal{ c: ('${instructionsToString(c)}' == $c | e: (${e.mkString(",")}) }"
        case EnvMachineValue(e, _) =>  s"EnvVal(${e.mkString(",")})"
        case VariableMachineValue(n, _) => s"$n"
        case _ => throwNotImplementedError
      }
      case v: MachineValue => v match {
        case ConstMachineValue(n, maybeVarName) => s"$n(${maybeVarName.getOrElse("-")})"
        case CodeMachineValue(c, maybeVarName) => s"CodeVal(${c.mkString(",")}, ${maybeVarName.getOrElse("-")}))"
        case ListMachineValue(values, _) => values.mkString("[" , ", ", "]")
        case _ => throwNotImplementedError
      }
      case _ => throwNotImplementedError
    }
  }

}
