package nfn.service

import java.nio.charset.StandardCharsets

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, MetaInfo}

import scala.util.{Failure, Success, Try}

/**
 * The map service is a generic service which transforms n [[NFNValue]] into a [[NFNListValue]] where each value was applied by a given other service of type [[NFNServiceValue]].
 * The first element of the arguments must be a [[NFNServiceValue]] and remaining n arguments must be a [[NFNListValue]].
 * The result of service invocation is a [[NFNListValue]].
 */
class MapService() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNStringValue = {
    args match {
      case Seq(function, arguments @ _*) => {
        val service = MapReduceService.serviceFromValue(function).get
        val values = arguments.map({ arg =>
          service.instantiateCallable(interestName, service.ccnName, Seq(arg), ccnApi, None).get.exec
        })
        NFNStringValue(MapReduceService.seqToString(values))
      }
      case _ =>
        throw new NFNServiceArgumentException(s"A Map service must match Seq(NFNServiceValue, NFNValue*), but it was: $args ")
    }
  }
}

/**
 * The reduce service is a generic service which transforms a [[NFNListValue]] into a single [[NFNValue]] with another given service.
 * The first element of the arguments must be a [[NFNServiceValue]] and the second argument must be a [[NFNListValue]].
 * The result of service invocation is a [[NFNValue]].
 */
class ReduceService() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(function, stringValue) => {
        val service = MapReduceService.serviceFromValue(function).get
        val arguments = MapReduceService.stringToSeq(stringValue match {
          case NFNStringValue(str) => str
          case NFNContentObjectValue(_, data) => new String(data)
          case _ =>
            throw new NFNServiceArgumentException(s"Second argument to ReduceService must be NFNStringValue or NFNContentObjectValue, but was: $stringValue")
        })

        service.instantiateCallable(interestName, service.ccnName, arguments, ccnApi, None).get.exec
      }
      case _ =>
        throw new NFNServiceArgumentException(s"A Reduce service must match Seq(service, value), but it was: $args")
    }
  }
}

object MapReduceService {
  def serviceFromValue(value: NFNValue): Try[NFNService] = {
    value match {
      case NFNServiceValue(service) => Success(service)
      case NFNContentObjectValue(name, data) => NFNService.serviceFromContent(Content(name, data, MetaInfo.empty))
      case _ => Failure(new NFNServiceArgumentException(s"NFNValue $value is not a valid NFNService."))
    }
  }

  def nfnValueToString(v: NFNValue): String = {
    val (typ, data) = v match {
      case NFNIntValue(i)     => ("Int", i.toString)
      case NFNStringValue(s)  => ("String", s)
      case NFNDataValue(d)    => ("Array[Byte]", new String(d, StandardCharsets.UTF_8))
      case NFNEmptyValue()    => ("EmptyValue", "")
      case NFNNameValue(name) => ("Name", name.toString)
      case _ => throw new NFNServiceArgumentException(s"Cannot turn NFNValue $v into string.")
    }
    s"$typ($data)"
  }

  def stringToNfnValue(s: String): NFNValue = {
    val typeDataRegex = """(?s)^([^(]*)\((.*)\)$""".r
    s match {
      case typeDataRegex(typ, data) => typ match {
        case "Int"         => NFNIntValue(data.toInt)
        case "String"      => NFNStringValue(data)
        case "Array[Byte]" => NFNDataValue(data.getBytes(StandardCharsets.UTF_8))
        case "EmptyValue"  => NFNEmptyValue()
        case "Name"        => NFNNameValue(CCNName.fromString(data).get)
        case _ => throw new NFNServiceArgumentException(s"Unknown type $typ of '$s'.")
      }
      case _ => throw new NFNServiceArgumentException(s"Cannot turn string '$s' into NFNValue. String must consist of 'type(data)'")
    }
  }

  def seqToString(values: Seq[NFNValue]): String = values.map(nfnValueToString).mkString("[", ",", "]")
  def stringToSeq(s: String): Seq[NFNValue] = {
    val listRegex = """(?s)^\[(.*)\]$""".r
    s.trim match {
      case listRegex(args) => args.split(',').map(stringToNfnValue)
      case _ =>
        throw new NFNServiceArgumentException(s"String must match '[arg, ..., arg]' but was '$s'")
    }
  }

}
