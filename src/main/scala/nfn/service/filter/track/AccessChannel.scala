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

   private def processAccessTrack(track:String, level:Int):String = {

     level match {
       case 0 =>
         "TODO"
       case _ =>
         throw new NFNServiceArgumentException(s"Invalid access level.")
     }

     "TODO"
   }

   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    println(">>>>> function in AccessChannel <<<<<")


     args match {
       case Seq(NFNStringValue(track), NFNIntValue(level)) =>
         NFNStringValue(processAccessTrack(track,level))

       case Seq(NFNContentObjectValue(_,track), NFNIntValue(level)) =>
         NFNStringValue(processAccessTrack(new String(track),level))

       case _ =>
         throw new NFNServiceArgumentException(s"Argument mismatch.")
     }

   }

 }
