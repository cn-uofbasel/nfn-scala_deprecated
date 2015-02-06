package nfn.service.filter.track

import akka.actor.ActorRef
import nfn.service._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Filter:
 *  Filtering of permissions for GPS tracks (access channel)
 *
 * Access Levels:
 *  0   Full information (no filtering)
 *
 */


class AccessChannel extends NFNService {

   private def processAccessTrack(request:String, level:Int):String = {

     level match {
       case 0 => {
        // TODO - take these information from database
        // TODO - more expressive requests
        request match {
          case "user1 track" => "0"      // full access
          case "user2 track" => "1"      // northpole filter
          case "user3 track" => "-1"     // no permissions
          case "processor track" => "0"  // full access
          case _ => "xxx" // TODO - no permissions
        }

       }

       case _ =>
         throw new NFNServiceArgumentException(s"Invalid access level.")
     }

   }

   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

     args match {
       case Seq(NFNStringValue(request), NFNIntValue(level)) =>
         NFNStringValue(processAccessTrack(request,level))

       case Seq(NFNContentObjectValue(_,request), NFNIntValue(level)) =>
         NFNStringValue(processAccessTrack(new String(request),level))

       case _ =>
         throw new NFNServiceArgumentException(s"Argument mismatch.")
     }

   }

 }
