package nfn.service.filter.track

import akka.actor.ActorRef
import nfn.service._

import scala.util.{Try, Failure, Success}
import ccn.packet._
import nfn.NFNApi

import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global

import net.liftweb.json._

/**
  * Created by Claudio Marxer <marxer@claudio.li>
  *
  * Filter:
  *  Filtering of GPS tracks (key channel)
  *
  */
class KeyChannel extends NFNService {

  /**
   * Check if certain user is allowed to access certain data at certain level
   * @param data Data extracted from the content object containing the permissions
   * @param node Identifier of the node
   * @param level Access level
   * @return True, if access is allowed. False, if access is denied or content object can not be parsed.
   */
  private def checkPermission(data: Array[Byte], node:String, level:Int): Boolean = {

    // classes to turn JSON into
    implicit val formats = DefaultFormats
    case class UserLevel(name:String, level:Int)
    case class Permissions(content:String, permissions:List[UserLevel])

    val json = new String(data)
    val triedParsedJson: Try[JValue] = Try(parse(json))

    triedParsedJson match {
      case Success(parsedJson) => {
        // parsing successfull, actual data extraction
        val permissions = parsedJson.extract[Permissions].permissions
        // NOTE: higher access permissions (lower access level) are also accepted
        (for (l <- 0 to level) yield (!permissions.find(_ == UserLevel(node, l)).isEmpty)) contains true
      }
      case Failure(e) => {
        // parsing failed
        false
      }
    }
  }

  /**
   *
   * @param track
   * @param node
   * @param level
   * @param ccnApi
   * @return
   */
   private def processKeyTrack(track:String, node:String, level:Int, ccnApi: ActorRef):String = {
     // fetch content object with permissions
     implicit val timeout = Timeout(2000)
     (ccnApi ? NFNApi.CCNSendReceive(Interest(CCNName(track.split("/").tail:_*)), false)).mapTo[Content].onComplete{

       // If content object with permissions received...
       case Success(c) => {
         checkPermission(c.data, node, level) match {
           case true => {
             // access allowed: return key
             "this-is-the-secret-key"
           }
           // return key
           case false => {
             // access denied: no not return key
             "no-key"
           }
         }
       }

       // If no content object returned...
       case Failure(exception) =>{
         "Failure"
       }

     }

    "xxx"

   }

  /**
   * Entry point of that service
   * @param args
   * @param ccnApi
   * @return
   */
   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

     args match {
       case Seq(NFNStringValue(track), NFNStringValue(node), NFNIntValue(level)) =>
         NFNStringValue(processKeyTrack(new String(track), new String(node), level, ccnApi))

       case _ =>
         throw new NFNServiceArgumentException(s"KeyChannel: Argument mismatch.")
     }

   }

 }
