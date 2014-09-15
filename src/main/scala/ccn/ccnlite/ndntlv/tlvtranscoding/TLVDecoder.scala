package ccn.ccnlite.ndntlv.tlvtranscoding

import java.nio.ByteBuffer

object TLVDecoder {

  def error(msg: String) = {
    throw new Exception(msg)
  }

  def errorWrongType(typeName: String, actual: Int, expected: Int) = {
    error(s"$typeName has type $expected and not $actual")
  }

  def warning(msg: String) = {
    println(msg)
  }

  def decodeTLVs(data: List[Byte]): List[TLV] = {
    val res =  decodeSingleTLV(data)
    res match {
      case Some((h, t)) => h :: decodeTLVs(t)
      case None => Nil
    }
  }

  def decodeTLVList(data: List[Byte]): List[TLV] = {
    decodeSingleTLV(data) match {
      case Some((tlv, tail)) =>  tlv :: decodeTLVList(tail)
      case None => Nil
    }
  }

  def nonNegativeInteger(d: List[Byte]): Long = {
    d match {
      case List(b1) if b1 < 0 => (b1 + 0x100).toShort
      case List(b1) => b1.toShort
      case List(b1) => b1.toLong
      case List(b1, b2) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, b1.toByte, b2.toByte)).getInt.toLong
      case List(b1, b2, b3, b4) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, b1, b2, b3, b4)).getLong
      case List(b1, b2, b3, b4, b5, b6, b7, b8) => {
        throw new Exception("ULong cannot be represented in the JVM with primitive datatypes, you would have to rely on a class representing UInt (bigint, a library or implement it yourself)")
      }
      case _ => throw new Exception(s"a nonnegnum in byte format has to have 1, 2, 4 or 8 bytes and not ${d.toList}")
    }
  }


  def varNumberToLong(d: List[Byte]): Long = {
    d match {
      case List(b) if b < 0 => (b + 0x100).toShort
      case List(b) => b.toShort
      case List(b1, b2) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, b1, b2)).getInt
      case List(b1, b2, b3, b4) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, b1, b2, b3, b4)).getLong
      case List(b1, b2, b3, b4, b5, b6, b7, b8) => {
        ByteBuffer.wrap(Array(b1, b2, b3, b3)).getLong
      }
      case _ => throw new Exception(s"a varnum in byte format has to have 1, 2, 4 or 8 bytes and not ${d.toList}")
    }
  }
  def decodeVarNumber(d: List[Byte]): (List[Byte], List[Byte]) = {
    d match {
      case h :: t =>
        h match {
          case b if b == -3 => {
            if(t.isEmpty) error("b == 253 but not enough following data")
            else t.splitAt(2)
          }
          case b if b == -2 => if (t.size < 2) error("varnumber: b == 254 but no enough following") else t.splitAt(4)
          case b if b == -1 => if (t.size < 4) error("varnumber: b == 254 but no enough following") else t.splitAt(8)

          case b => (List(h), t)
        }
      case _ => error("varNumber: no data")
    }
  }

  def decodeSingleTLV(data: List[Byte]): Option[(TLV, List[Byte])] = {

    data match {
      case Nil => None
      case List(dataTLV@ _*) => {
        val (typVN, dataLV) = decodeVarNumber(dataTLV.toList)
        val typ = varNumberToLong(typVN)

        val (lengthVN, dataV) = decodeVarNumber(dataLV)
        val length = varNumberToLong(lengthVN)

        val (value, data_) = {

          if(length > Integer.MAX_VALUE) println("warning: cut of length in array because of sign")

          val (dataList, data_) = dataV.splitAt(length.toInt)
          if(dataList.size != length) error(s"data field not long enough: expected_length=$length datafield_length=${dataList.size} '${new String(dataList.toArray)}'")
          (dataList, data_)
        }

        Some(TLV(typ, length, value), data_)
      }
    }
  }
}




