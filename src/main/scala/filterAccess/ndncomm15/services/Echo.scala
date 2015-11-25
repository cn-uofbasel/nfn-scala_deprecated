package filterAccess.ndncomm15.services

import javax.crypto.{Cipher, KeyGenerator}

import akka.actor.ActorRef
import akka.util.Timeout
import ccn.packet.{Interest, Content, CCNName}
import nfn.NFNApi
import akka.pattern._
import nfn.service._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.io.Source
import scala.util._
import nfn.tools.Helpers._

class Echo extends NFNService{

  val access_control_file = "ac.txt"

  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    val data = args.head
    val id = args.tail.head


    data match {
      case NFNStringValue(d) => {
        id match {
          case NFNStringValue(i) => {
            val ac_lines = Source.fromFile(access_control_file).getLines()
            val ac_lines_split = ac_lines.map(x => x.split(","))
            val key =  (ac_lines_split.find(x => x.head == i))

            key match{
              case Some(ek) => {
                if(ek.tail.head == "n"){
                  val filename = ek.tail.tail.head
                  val networkname = CCNName(filename.substring(1).split("/").toList, None)
                  NFNNameValue(networkname)
                  val interest = Interest(networkname)

                  import ExecutionContext.Implicits.global
                  implicit val timeout = Timeout(10000)
                  val fres: Future[Content] = (ccnApi ? NFNApi.CCNSendReceive(interest, false)).mapTo[Content]
                  val res = Await.result(fres, timeout.duration)

                  res match{
                    case c: Content => {
                      val ek = byteToString(c.data)
                      val encrypted: String = filterAccess.crypto.Encryption.symEncrypt(d, ek)
                      NFNStringValue(encrypted)
                    }
                    case _ => NFNStringValue("Key file not found")
                  }
                }
                else{
                  val encrypted: String = filterAccess.crypto.Encryption.symEncrypt(d, ek.tail.tail.head)
                  NFNStringValue(encrypted)
                }

              }
              case _ => NFNStringValue("Unknown ID, create ID first")
            }
          }
          case _ => ???
        }
      }
      case _ => ???
    }
  }
}