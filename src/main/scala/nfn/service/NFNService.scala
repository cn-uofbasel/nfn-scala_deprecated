package nfn.service

import java.io.{File, FileOutputStream}

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import bytecode.BytecodeLoader
import ccn.packet._
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.ConfigFile
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.Logging
import config.{StaticConfig, AkkaConfig}
import lambdacalculus.parser.ast._
import myutil.IOHelper
import nfn.NFNApi
import nfn.KlangCancellableFuture

import scala.concurrent._
import scala.util.{Failure, Success, Try}

object NFNService extends Logging {

  implicit val timeout = Timeout(StaticConfig.defaultTimeoutDuration)

  /**
   * Creates a [[NFNService]] from a content object containing the binary code of the service.
   * The data is written to a temporary file, which is passed to the [[BytecodeLoader]] which then instantiates the actual class.
   * @param content
   * @return
   */
  def serviceFromContent(content: Content): Try[NFNService] = {
    val serviceLibraryDir = "./temp-service-library"
    val serviceLibararyFile = new File(serviceLibraryDir)

    if(serviceLibararyFile.exists) {
      serviceLibararyFile.mkdir
    }

    def createTempFile: File = {
      val f = new File(s"$serviceLibraryDir/tempserv${System.nanoTime}")
      if (!f.exists) {
        f
      } else {
        Thread.sleep(1)
        createTempFile
      }
    }

    val file: File = createTempFile

    try {
      val out = new FileOutputStream(file)
      val filePath = file.getCanonicalPath
      try {

        out.write(content.data)
        out.flush()
      } finally {
        if (out != null) out.close
      }

      val servName = content.name.cmps.last.replace("_", ".")
      val loadedService: Try[NFNService] = BytecodeLoader.loadClass[NFNService](filePath, servName)
      logger.debug(s"Dynamically loaded class $servName from content")
      loadedService
    } finally {
      if (file.exists) file.delete
    }
  }

  def parseAndFindFromName(name: String, ccnServer: ActorRef)(implicit ec: ExecutionContext): Future[CallableNFNService] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      (ccnServer ? NFNApi.CCNSendReceive(interest, useThunks = false)).mapTo[Content]
    }

    def findService(fun: String): Future[NFNService] = {
      logger.debug(s"Looking for service $fun")
      CCNName.fromString(fun) match {
        case Some(CCNName(cmps, _)) =>
          val interest = Interest(CCNName(cmps, None))
          val futServiceContent: Future[Content] = loadFromCacheOrNetwork(interest)

          import myutil.Implicit.tryToFuture
          futServiceContent flatMap { serviceFromContent }
        case None => Future.failed(new Exception(s"Could not create name for service $fun"))
      }
    }

    def findArgs(args: List[Expr]): Future[List[NFNValue]] = {
      logger.debug(s"Looking for args ${args.mkString("[ ", ", ", " ]")}")
      Future.sequence(
        args map { (arg: Expr) =>
          arg match {
            case Constant(i) =>  Future( NFNIntValue(i) )
            case Str(s) => Future( NFNStringValue(s))
            case otherExpr @ _ => {
              import nfn.LambdaNFNImplicits._
              val maybeInterest = otherExpr match {
                case Variable(varName, _) => {
                  CCNName.fromString(varName) map {
                    Interest(_)
                  }
                }
                case _ => Some(NFNInterest(otherExpr))
              }

              maybeInterest match {
                case Some(interest) => {
                  logger.debug(s"Arg '$arg' is a name, asking the ccnServer to find content for $interest")
                  val foundContent: Future[NFNContentObjectValue] = loadFromCacheOrNetwork(interest) map  { content =>
                    logger.debug(s"Found $content for arg $arg")
                    NFNContentObjectValue(content.name, content.data)
                  }

                  foundContent.onFailure {
                    case error => logger.error(s"Could not find content for arg $arg", error) // send keepalive interest
                  }

                  foundContent
                }
                case None => {
                  val errorMsg = s"Could not created interest for arg $arg)"
                  logger.error(errorMsg)
                  Future.failed(new Exception(errorMsg))
                }
              }
            }
          }
        }
      )
    }

    val lc = lambdacalculus.LambdaCalculus()

    lc.parse(name) match {
      case Success(parsedExpr) =>
          parsedExpr match {
            case Call(funName, argExprs) => {

              // find service
              val futServ: Future[NFNService] = findService(funName)

              // create or find values for args
              val futArgs: Future[List[NFNValue]] = findArgs(argExprs)

              import myutil.Implicit.tryToFuture
              val futCallableServ: Future[CallableNFNService] =
                for {
                  args <- futArgs
                  serv <- futServ
                  callable <- serv.instantiateCallable(CCNName(name), serv.ccnName, args, ccnServer, serv.executionTimeEstimate)
                } yield callable


              futCallableServ onSuccess {
                case callableServ => logger.info(s"Instantiated callable serv: '$name' -> $callableServ")
              }
              futCallableServ
            }
            case _ => throw new Exception("call is the only valid expression for a COMPUTE request")
          }
      case Failure(ex) => {
        Future.failed(ex)
      }
    }
  }
}

trait NFNService {

  def executionTimeEstimate: Option[Int] = None

  def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue

  def instantiateCallable(interestName: CCNName, name: CCNName, values: Seq[NFNValue], ccnServer: ActorRef, executionTimeEstimate: Option[Int]): Try[CallableNFNService] = {
    assert(name == ccnName, s"Service $ccnName is created with wrong name $name")
    Try(CallableNFNService(interestName, name, values, ccnServer, (interestName, args, ccnApi) => function(interestName, args, ccnApi), executionTimeEstimate = executionTimeEstimate))
  }
//  def instantiateCallable(name: NFNName, futValues: Seq[Future[NFNServiceValue]], ccnWorker: ActorRef): Future[CallableNFNService]

  def ccnName: CCNName = CCNName(this.getClass.getCanonicalName.replace(".", "_"))

  def pinned: Boolean = false

  override def toString = ccnName.toString

}

class ServiceException(msg: String) extends Exception(msg)

case class NFNServiceExecutionException(msg: String) extends ServiceException(msg)
case class NFNServiceArgumentException(msg: String) extends ServiceException(msg)

case class CallableNFNService(interestName: CCNName, name: CCNName, values: Seq[NFNValue], nfnMaster: ActorRef, function: (CCNName, Seq[NFNValue], ActorRef) => NFNValue, executionTimeEstimate: Option[Int]) extends Logging {
  def exec:NFNValue = function(interestName, values, nfnMaster)
}

abstract class NFNDynamicService() extends NFNService {
  override def ccnName = CCNName("nfn_DynamicService")
}
