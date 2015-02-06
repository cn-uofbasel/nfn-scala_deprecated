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
     println(">>>>> processAccessTrack <<<<<")
     "TODO"
   }

   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

     args match {
       case Seq(NFNStringValue(track), NFNIntValue(level)) =>
         NFNStringValue(processAccessTrack(track,level))

       case _ =>
         throw new NFNServiceArgumentException(s"Argument mismatch.")
     }

   }

 }
