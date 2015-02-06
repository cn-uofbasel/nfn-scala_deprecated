package nfn.service.filter.track

import akka.actor.ActorRef
import nfn.service._

/**
  * Created by Claudio Marxer <marxer@claudio.li>
  *
  * Filter:
  *  Filtering of GPS tracks (key channel)
  *
  */


class KeyChannel extends NFNService {

   private def processKeyTrack(track:String, id:String, level:Int):String = {
     /* TODO
      *  (1) Fetch Permission
      *  (2) Compare with level
      *  (3) If permitted: Send SymKey
      *
      */
   }

   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

     args match {
       case Seq(NFNStringValue(track), NFNStringValue(id), NFNIntValue(level)) =>
         NFNStringValue(processKeyTrack(new String(track), new String(id), level))

       case _ =>
         throw new NFNServiceArgumentException(s"KeyChannel: Argument mismatch.")
     }

   }

 }
