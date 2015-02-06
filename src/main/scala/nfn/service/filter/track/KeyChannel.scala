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

   private def processKeyTrack(track:String, level:Int):String = {
     ???
   }

   override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
     ???
   }

 }
