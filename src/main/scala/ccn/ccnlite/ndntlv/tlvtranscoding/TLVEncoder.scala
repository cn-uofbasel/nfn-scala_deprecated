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
      // TODO unsigned with 8Byte is not implmented, but this is also true for intToVarNum in the decoder
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

  def intToVarNum(value: Int): List[Byte] = {
    value match {
      case i if i < 253 => List(i.toByte)
      case i if i <= Short.MaxValue => {
        val bb = ByteBuffer.allocate(3)
        bb.put(253.toByte)
        bb.putShort(i.toShort)
        bb.array().toList
      }
      case i @ _ => {
        val bb = ByteBuffer.allocate(5)
        bb.put(254.toByte)
        bb.putInt(i)
        bb.array().toList
      }
    }
  }


   def encode(tlv: TLV): List[Byte] = {
     val t = intToVarNum(tlv.t)
     val l = intToVarNum(tlv.l)
     val v = tlv.v

     t ::: l ::: v
   }
 }
