package ccn.ccnlite.ndntlv

import java.nio.ByteBuffer

import myutil.IOHelper

object TLVDecoder {


  trait NDNTLVType

  trait PacketType

  trait RecursingType



// Packet types
  case class Interest() extends NDNTLVType with PacketType with RecursingType
  case class Data() extends NDNTLVType with PacketType with RecursingType
//  Common fields
  case class Name()	extends NDNTLVType with RecursingType
  case class NameComponent() extends NDNTLVType
//  Interest packet
  case class Selectors() extends NDNTLVType with RecursingType
  case class Nonce()	extends NDNTLVType
  case class Scope()	extends NDNTLVType
  case class InterestLifetime()	extends NDNTLVType

  trait Selector
//  Interest/Selectors
  case class MinSuffixComponents() extends NDNTLVType with Selector
  case class MaxSuffixComponents() extends NDNTLVType with Selector
  case class PublisherPublicKeyLocator() extends NDNTLVType with Selector
  case class Exclude() extends NDNTLVType with Selector
  case class ChildSelector() extends NDNTLVType with Selector
  case class MustBeFresh() extends NDNTLVType with Selector
  case class Any() extends NDNTLVType with Selector
//  Data packet
  case class MetaInfo() extends NDNTLVType with RecursingType
  case class Content() extends NDNTLVType
  case class SignatureInfo() extends NDNTLVType with RecursingType
  case class SignatureValue() extends NDNTLVType
//  Data/MetaInfo
  case class ContentType() extends NDNTLVType
  case class FreshnessPeriod() extends NDNTLVType
  case class FinalBlockId() extends NDNTLVType
//  Data/Signature
  case class SignatureType() extends NDNTLVType
  case class KeyLocator() extends NDNTLVType with RecursingType
  case class KeyDigest() extends NDNTLVType
  case class UnkownT() extends NDNTLVType

  case class TLV(t: NDNTLVType, l: Int, v: List[Byte])

  def error(msg: String) = {
    throw new Exception(msg)
  }
  def decodeSingleTLV(data: List[Byte]): (TLV, List[Byte]) = {
    def varNumberToInt(d: List[Byte]): Int = {
      d match {
        case List(b) => b
        case List(b1, b2) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, b1, b2)).getInt
        case List(b1, b2, b3, b4) => ByteBuffer.wrap(Array(b1, b2, b3, b3)).getInt
      }

    }

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

    val dataTLV = data

    val (typVN, dataLV) = varNumber(dataTLV)
    //      println(s"typ: ${varNumberToInt(typ)}")
    val typT = typToT(varNumberToInt(typVN))
    val (lengthVN, dataV) = varNumber(dataLV)
    //      println(s"length: ${varNumberToInt(lengthVN)}")
    val length = varNumberToInt(lengthVN)
    val (value, data_) = {
      val (dataList, data_) = dataV.splitAt(length)
      if(dataList.size != length) error(s"data field not long enough: length=$length datafield='${new String(dataList.toArray)}'")
      (dataList, data_)
    }

    (TLV(typT, length, value), data_)
  }
  def typToT(typ: Int): NDNTLVType = {
    typ match {
      case 5 => Interest()
      case 6 => Data()
      case 7 => Name()
      case 8 => NameComponent()
      case 9 => Selectors()
      case 10 => Nonce()
      case 11 => Scope()
      case 12 => InterestLifetime()
      case 13 => MinSuffixComponents()
      case 14 => MaxSuffixComponents()
      case 15 => PublisherPublicKeyLocator()
      case 16 => Exclude()
      case 17 => ChildSelector()
      case 18 => MustBeFresh()
      case 19 => Any()
      case 20 => MetaInfo()
      case 21 => Content()
      case 22 => SignatureInfo()
      case 23 => SignatureValue()
      case 24 => ContentType()
      case 25 => FreshnessPeriod()
      case 26 => FinalBlockId()
      case 27 => SignatureType()
      case 28 => KeyLocator()
      case 29 => KeyDigest()
      case _ => throw new Exception(s"unkown type $typ")
    }
  }

  def decode(dataArr: Seq[Byte], lvl: Int): Unit = {


    def printlnLvl(msg: String, l: Int) = println(" "*l*2 + msg)

    if(dataArr.size == 0) return

    val (tlv, tail) = decodeSingleTLV(dataArr.toList)

    printlnLvl(tlv.t.toString, lvl)
    if(tlv.t.isInstanceOf[RecursingType]) {
      decode(tlv.v, lvl + 1)
    } else {
      val a = tlv.v.toArray
      printlnLvl(s"'${new String(a)}' (s=${a.size})", lvl + 1)
    }

    decode(tail, lvl)
  }
}

object TLVTest extends App {
  val ba = IOHelper.readByteArrayFromFile("./asdf_asdf.ndn")

  println(s"ba (s=${ba.size})")
  TLVDecoder.decode(ba, 0)
}
