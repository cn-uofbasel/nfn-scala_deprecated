package nfn.service.filter.track

import akka.actor.ActorRef
import nfn.service._

import scala.util.{Failure, Success}
import ccn.packet._
import nfn.NFNApi

import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by Claudio Marxer <marxer@claudio.li>
  *
  * Filter:
  *  Filtering of GPS tracks (key channel)
  *
  */


class KeyChannel extends NFNService {

   private def processKeyTrack(track:String, id:String, level:Int, ccnApi: ActorRef):String = {

     /*
     (ccnApi ? NFNApi.CCNSendReceive(Interest("node/node1/trackPermission"), false)).mapTo[Content].onComplete {
       case Success(contentObj) => println(">>>>>>>>> SUCCESS")
       case Failure(exception) => println(">>>>>>>>> Error: " + exception)
     }
     */

     // fetch content object
     implicit val timeout = Timeout(20000)

     // TODO fix timeout exception
     (ccnApi ? NFNApi.CCNSendReceive(Interest("node/node1/trackPermission"), false)).mapTo[Content].onComplete{

       case Success(c) => {

         // TODO retrieve as content object, see timeout exception
         val permissionData = Map(
           ("user1", "trackname") -> 0,
           ("user2", "trackname") -> 1,
           ("processor", "trackname") -> 0
         )
         // compare with actual permission level
         permissionData((id, track)) != -1 && permissionData((id, track)) <= level match {
           case true =>
             // send key
             "KEY"
           case _ =>
             // do not send key
             "NO-KEY"
         }
       }

       case Failure(exception) =>
         "ERROR"

       case _ =>
         "ERROR"

     }

     "XXX"

   }

   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

     args match {
       case Seq(NFNStringValue(track), NFNStringValue(id), NFNIntValue(level)) =>
         NFNStringValue(processKeyTrack(new String(track), new String(id), level, ccnApi))

       case _ =>
         throw new NFNServiceArgumentException(s"KeyChannel: Argument mismatch.")
     }

   }

 }
