package nfn.service

import javax.crypto.{Cipher, KeyGenerator}

import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.io.Source

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
                  val networkname = CCNName(filename.substring(1))
                  NFNNameValue(networkname)
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