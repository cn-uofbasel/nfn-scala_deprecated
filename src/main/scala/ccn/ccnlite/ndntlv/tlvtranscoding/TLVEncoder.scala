package ccn.ccnlite.ndntlv.tlvtranscoding

import java.nio.ByteBuffer

object TLVEncoder {

  def longToNonNegativeInteger(l: Long): List[Byte] = {
    l match {
      case i if i <= 255 => List(i.toByte)
      case i if i <= 65535 => {
        val bb = ByteBuffer.allocate(4)
        bb.putInt(i.toInt)
        bb.array.splitAt(2)._2.toList
      }
      case i@_ => {
        val bb = ByteBuffer.allocate(8)
        bb.putLong(i)
        bb.array.splitAt(4)._2.toList
      }
      // TODO unsigned with 8Byte is not implemented, but this is also true for intToVarNum in the decoder
    }
  }

  def encode(tlvs: List[Option[TLV]]): List[Byte] = {
     tlvs match {
       case h :: t =>
         h match {
           case Some(tlv) => encode(tlv) ::: encode(t)
           case None => encode(t)
         }
       case Nil => Nil
     }
   }

  def longToVarNum(value: Long): List[Byte] = {
    value match {
      case i if i < 253 => { List(i.toByte) }

      case i if i < 65536 => {
        val head = 253.toByte
        val b1 = i.toByte
        val b2 = (i >> 8).toByte
        val r: List[Byte] = List(head, b2, b1)
        r
      }
      case i => {
        val head = 254.toByte
        val b1 = i.toByte
        val b2 = (i >> 8).toByte
        val b3 = (i >> 16).toByte
        val b4 = (i >> 24).toByte
        List(head, b4, b3, b2, b1)
      }
    }
  }


   def encode(tlv: TLV): List[Byte] = {
     val t = longToVarNum(tlv.t)
     val l = longToVarNum(tlv.l)
     val v = tlv.v

     t ::: l ::: v
   }
 }
