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
      case List(b1) => b1
      case List(b1, b2) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, b1.toByte, b2.toByte)).getInt
      case List(b1, b2, b3, b4) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, b1, b2, b3, b4)).getLong
      case List(b1, b2, b3, b4, b5, b6, b7, b8) => {
        throw new Exception("ULong is not supported in the JVM, you need to get a library or use bigint if you really need such a large number")
        ByteBuffer.wrap(Array(b1, b2, b3, b4, b5, b6, b7, b8)).getLong
      }
    }
  }

  def varNumberToInt(d: List[Byte]): Int = {
    d match {
      case List(b) => b
      case List(b1, b2) => ByteBuffer.wrap(Array(b1, b2)).getShort
      case List(b1, b2, b3, b4) => ByteBuffer.wrap(Array(b1, b2, b3, b3)).getInt
    }

  }

  def decodeSingleTLV(data: List[Byte]): Option[(TLV, List[Byte])] = {
    def varNumber(d: List[Byte]): (List[Byte], List[Byte]) = {
      d match {
        case h :: t =>
          h match {
            case b if b < 253 => (List(b), t)
            case b if b == 253 => if(t.isEmpty) error("b == 253 but not enough following data")        else t.splitAt(1)
            case b if b == 254 => if (t.size < 2) error("varnumber: b == 254 but no enough following") else t.splitAt(2)
            case b if b == 255 => if (t.size < 4) error("varnumber: b == 254 but no enough following") else t.splitAt(4)
          }
        case _ => error("varNumber: no data")
      }
    }

    data match {
      case Nil => None
      case List(dataTLV@ _*) => {
        val (typVN, dataLV) = varNumber(dataTLV.toList)
          //      println(s"typ: ${varNumberToInt(typ)}")
        val typ = varNumberToInt(typVN)
        val (lengthVN, dataV) = varNumber(dataLV)
          //      println(s"length: ${varNumberToInt(lengthVN)}")
        val length = varNumberToInt(lengthVN)
        val (value, data_) = {
          val (dataList, data_) = dataV.splitAt(length)
          if(dataList.size != length) error(s"data field not long enough: length=$length datafield='${new String(dataList.toArray)}'")
          (dataList, data_)
        }

        Some(TLV(typ, length, value), data_)
      }
    }
  }
}




