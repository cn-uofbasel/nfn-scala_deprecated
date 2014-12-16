package nfn.localAbstractMachine

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.util.Failure

import akka.actor.ActorRef

import lambdacalculus.machine._
import nfn.service._
import lambdacalculus.machine.CallByValue.VariableMachineValue

case class LocalNFNCallExecutor(ccnWorker: ActorRef)(implicit execContext: ExecutionContext) extends CallExecutor {

  override def executeCall(call: String): MachineValue = {

    val futValue: Future[MachineValue] = {
      for {
        callableServ <- NFNService.parseAndFindFromName(call, ccnWorker)
      } yield {
        val result = callableServ.exec
        NFNValueToMachineValue.toMachineValue(result)
      }
    }
    Await.result(futValue, 20.seconds)
  }
}

object NFNValueToMachineValue {
  def toMachineValue(nfnValue: NFNValue):MachineValue =  {

    nfnValue match {
      case NFNIntValue(n) => ConstMachineValue(n)
      case NFNNameValue(name) => VariableMachineValue(name.toString)
      case NFNEmptyValue() => NopMachineValue()
      case NFNListValue(values: List[NFNValue]) => ListMachineValue(values map { toMachineValue})
      case _ =>  throw new Exception(s"NFNValueToMachineValue: conversion of $nfnValue to machine value type not implemented")
    }
  }

}

