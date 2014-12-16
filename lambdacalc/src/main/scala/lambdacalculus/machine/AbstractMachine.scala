package lambdacalculus.machine

import com.typesafe.scalalogging.slf4j.Logging
import lambdacalculus.parser.ast.Call
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.util.{Failure, Success, Try}

import scala.concurrent.duration._

/**
 * Represents a single state of execution
 */
trait Configuration {
  def isTransformable: Boolean
}

case class MachineException(msg: String) extends Exception(msg)

trait CallExecutor {
  def apply(call: String): MachineValue = executeCall(call)
  def executeCall(call: String): MachineValue
}

abstract class AbstractMachine(val storeIntermediateSteps:Boolean = false) extends Logging {

  type AbstractConfiguration <: Configuration

  def maybeExecutor: Option[CallExecutor]

  var _intermediateConfigurations: List[AbstractConfiguration] = List()

  def apply(code:List[Instruction]):List[MachineValue] = {
    logger.info(s"Executing code: $code")
    result(step(startCfg(code)))
  }

  def startCfg(code: List[Instruction]): AbstractConfiguration

  def result(cfg: AbstractConfiguration): List[MachineValue]

  def transform(state:AbstractConfiguration): AbstractConfiguration

  @tailrec
  private def step(cfg: AbstractConfiguration):AbstractConfiguration = {
    logger.debug(cfg.toString)
    if(storeIntermediateSteps) _intermediateConfigurations ::= cfg
    if(cfg.isTransformable) {
      step(transform(cfg))
    } else {
      cfg
    }
  }

  def printIntermediateSteps() =
    if(storeIntermediateSteps) {
      println(_intermediateConfigurations.reverse.mkString("\n===>\n"))
    } else {
      logger.error("Can only print intermediate steps if storeIntermediateSteps is set to true")
    }


  def intermediateConfigurations:Option[List[Configuration]] = if(storeIntermediateSteps) Some(_intermediateConfigurations) else None


  protected def executeCall(call: String): MachineValue = {
    maybeExecutor match {
      case Some(exec) => exec.executeCall(call)
      case None => throw new MachineException("This AbstractMachine needs a CallExecutor if it should be able to execute call commands")
    }
  }
}

