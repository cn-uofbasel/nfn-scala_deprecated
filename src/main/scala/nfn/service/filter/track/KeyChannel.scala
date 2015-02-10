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

     // fetch content object
     implicit val timeout = Timeout(2000)

     // TODO fix timeout exception
     (ccnApi ? NFNApi.CCNSendReceive(Interest(CCNName(track.split("/").tail:_*)), false)).mapTo[Content].onComplete{

       case Success(c) => {
         println(">>> SUCCESS")
       }

       case Failure(exception) =>{
         println(">>> ERROR:" + exception)
       }

     }

     "TODO"

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
