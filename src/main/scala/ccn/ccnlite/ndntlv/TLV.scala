package ccn.ccnlite.ndntlv

import java.nio.ByteBuffer

import ccn.ccnlite.ndntlv.TLVDecoder.PacketType
import myutil.IOHelper

object TLVDecoder {

  trait NDNTLVType {
    def typ: Int
  }

  trait NDNTLVTypeBuilder[A >: NDNTLVType] {
    def from(tlv: TLV): A
    def verify(tlv: TLV, typ: Int)(f: List[Byte] => A): A = {
      if(tlv.t == typ) f(tlv.v)
      else errorWrongType(this.getClass.getName, typ, tlv.t)
    }

    def dataToTypeAndTail[A >: NDNTLVType](data: List[Byte], typBuilder: NDNTLVTypeBuilder[A]): (A, List[Byte]) = {
      val (nextTlv, tail) = decodeSingleTLV(data)
      val typ: A = typBuilder.from(nextTlv)
      (typ, tail)
    }

    def maybeDataToTypeAndTail[A >: NDNTLVType](data: List[Byte], typBuilder: NDNTLVTypeBuilder[A]): (Option[A], List[Byte]) = {
      if(data.isEmpty) (None, data)
      else {
        val (a, tail) = dataToTypeAndTail(data, typBuilder)
        (Some(a), tail)
      }

    }

  }

  object PacketType {
    def from(data: List[Byte]): NDNTLVType = {
      val (tlv, tail) = decodeSingleTLV(data)
      tlv.t match {
        case 5 => Interest.from(tlv)
        case 6 => Data.from(tlv)
        case _ => error(s"PacketType can only be build form 5 or 6 and not ${tlv.t}")
      }
    }
  }
  trait PacketType extends NDNTLVType

  trait RecursingType


  // Packet types
  object Interest extends NDNTLVTypeBuilder[Interest] {

    override def from(tlv: TLV): Interest = {
      verify(tlv, 5) { v =>
        val (name, tailName) = dataToTypeAndTail(v, Name)
        val (selectors, tailSelectors) = dataToTypeAndTail(tailName, Selectors)
        val (nonce, tailNonce) = dataToTypeAndTail(tailSelectors, Nonce)
        val (scope, tailScope) = maybeDataToTypeAndTail(tailNonce, Scope)
        val (interestLifetime, tailIL) = maybeDataToTypeAndTail(tailScope, InterestLifetime)
        Interest(name, selectors, nonce, scope, interestLifetime)
      }
    }

  }
  case class Interest(name: Name, selectors: Selectors, nonce: Nonce, scope: Option[Scope], interestLifetime: Option[InterestLifetime]) extends NDNTLVType with PacketType with RecursingType {
    val typ = 5
  }


  object Data extends NDNTLVTypeBuilder[Data] {
    override def from(tlv: TLV): Data = {
      verify(tlv, 6) { v => Data()}
    }
  }
  case class Data() extends NDNTLVType with PacketType with RecursingType {
    val typ = 6
  }
  //  Common fields

  object Name extends NDNTLVTypeBuilder[Name] {
    def from(tlv: TLV): Name = {
      verify(tlv, 7) { v =>
        val nameComps = decodeTLVList(v) map { NameComponent.from }
        Name(nameComps)
      }
    }
  }
  case class Name(nameComps: List[NameComponent])	extends NDNTLVType with RecursingType {
    val typ = 7
  }

  object NameComponent extends NDNTLVTypeBuilder[NameComponent] {
    def from(tlv: TLV): NameComponent = {
      verify(tlv, 8) { v =>
        NameComponent(new String(v.toArray))
      }

    }
  }
  case class NameComponent(nameComp: String) extends NDNTLVType {
    val typ = 8
  }
  //  Interest packet

  object Selectors extends NDNTLVTypeBuilder[Selectors] {
    def from(tlv: TLV): Selectors = {
      verify(tlv, 9) { v =>
        val (tlvMinSC, tailMinSC) = maybeDataToTypeAndTail(v, MinSuffixComponents)
        val (tlvMaxSC, tailMaxSC) = maybeDataToTypeAndTail(tailMinSC, MaxSuffixComponents)
        val (tlvPPKL, tailPPKL) = maybeDataToTypeAndTail(tailMaxSC, PublisherPublicKeyLocator)
        val (tlvExclude, tailExclude) = maybeDataToTypeAndTail(tailPPKL, Exclude)
        val (tlvCS, tailCS) = maybeDataToTypeAndTail(tailExclude, ChildSelector)
        val (tlvMBF, tailMBF) = maybeDataToTypeAndTail(tailCS, MustBeFresh)
        Selectors(tlvMinSC, tlvMaxSC, tlvPPKL, tlvExclude, tlvCS, tlvMBF)
      }
    }
  }
  case class Selectors(
                        minSuffixComponents: Option[MinSuffixComponents],
                        maxSuffixComponents: Option[MaxSuffixComponents],
                        publisherPublicKeyLocator: Option[PublisherPublicKeyLocator],
                        exclude: Option[Exclude],
                        childSelector: Option[ChildSelector],
                        mustBeFresh: Option[MustBeFresh]) extends NDNTLVType with RecursingType {
    val typ = 9
  }

  object Nonce extends NDNTLVTypeBuilder[Nonce] {
    def from(tlv: TLV): Nonce = {
      verify(tlv, 10) { v => Nonce(v.toArray)}
    }
  }
  case class Nonce(nonce: Array[Byte])	extends NDNTLVType {
    val typ = 9
  }

  object Scope extends NDNTLVTypeBuilder[Scope] {
    override def from(tlv: TLV): Scope = {
      verify(tlv, 11) {v =>Scope(TLVDecoder.nonNegativeInteger(v)) }
    }
  }

  case class Scope(scope: Int)	extends NDNTLVType {
    val typ = 11
  }
  object InterestLifetime extends NDNTLVTypeBuilder[InterestLifetime] {
    override def from(tlv: TLV): InterestLifetime = {
      verify(tlv, 12) {v => InterestLifetime(TLVDecoder.nonNegativeInteger(v)) }
    }
  }
  case class InterestLifetime(interestLifetime: Int)	extends NDNTLVType {
    val typ = 12
  }

  trait Selector
  //  Interest/Selectors

  object MinSuffixComponents  extends NDNTLVTypeBuilder[MinSuffixComponents] {
    override def from(tlv: TLV): MinSuffixComponents = {
      verify(tlv, 13) {v => MinSuffixComponents(TLVDecoder.nonNegativeInteger(v)) }
    }
  }
  case class MinSuffixComponents(v: Int) extends NDNTLVType with Selector {
    val typ = 13
  }

  object MaxSuffixComponents extends NDNTLVTypeBuilder[MaxSuffixComponents] {
    override def from(tlv: TLV): MaxSuffixComponents = {
      verify(tlv, 14) {v => MaxSuffixComponents(TLVDecoder.nonNegativeInteger(v)) }
    }
  }
  case class MaxSuffixComponents(v: Int) extends NDNTLVType with Selector {
    val typ = 14
  }

  object PublisherPublicKeyLocator  extends NDNTLVTypeBuilder[PublisherPublicKeyLocator] {
    override def from(tlv: TLV): PublisherPublicKeyLocator = {
      verify(tlv, 15) {_ => PublisherPublicKeyLocator() }
    }
  }
  case class PublisherPublicKeyLocator(/*TODO: KeyLocator*/) extends NDNTLVType with Selector {
    val typ = 15
  }

  object Exclude extends NDNTLVTypeBuilder[Exclude] {
    override def from(tlv: TLV): Exclude = {
      verify(tlv, 15) {_ => Exclude()}
    }
  }
  case class Exclude() extends NDNTLVType with Selector {
    val typ = 16
  }

  object ChildSelector  extends NDNTLVTypeBuilder[ChildSelector] {
    override def from(tlv: TLV): ChildSelector = ???
  }
  case class ChildSelector() extends NDNTLVType with Selector {
    val typ = 17
  }

  object MustBeFresh extends NDNTLVTypeBuilder[MustBeFresh] {
    override def from(tlv: TLV): MustBeFresh = ???
  }
  case class MustBeFresh() extends NDNTLVType with Selector {
    val typ = 18
  }

  object Any extends  NDNTLVTypeBuilder[Any] {
    override def from(tlv: TLV): Any = ???
  }
  case class Any() extends NDNTLVType with Selector {
    val typ = 19
  }
  //  Data packet

  object MetaInfo extends NDNTLVTypeBuilder[MetaInfo] {
    override def from(tlv: TLV): MetaInfo = ???
  }
  case class MetaInfo() extends NDNTLVType with RecursingType {
    val typ = 20
  }

  object Content extends NDNTLVTypeBuilder[Content] {
    override def from(tlv: TLV): Content = ???
  }
  case class Content() extends NDNTLVType {
    val typ = 21
  }

  object SignatureInfo  extends NDNTLVTypeBuilder[SignatureInfo] {
    override def from(tlv: TLV): SignatureInfo = ???
  }
  case class SignatureInfo() extends NDNTLVType with RecursingType {
    val typ = 22
  }

  object SignatureValue  extends NDNTLVTypeBuilder[SignatureValue] {
    override def from(tlv: TLV): SignatureValue = ???
  }
  case class SignatureValue() extends NDNTLVType {
    val typ = 23
  }
  //  Data/MetaInfo

  object ContentType  extends NDNTLVTypeBuilder[ContentType] {
    override def from(tlv: TLV): ContentType = ???
  }
  case class ContentType() extends NDNTLVType {
    val typ = 24
  }

  object FreshnessPeriod extends NDNTLVTypeBuilder[FreshnessPeriod] {
    override def from(tlv: TLV): FreshnessPeriod = ???
  }
  case class FreshnessPeriod() extends NDNTLVType {
    val typ = 25
  }

  object FinalBlockId extends NDNTLVTypeBuilder[FinalBlockId] {
    override def from(tlv: TLV): FinalBlockId = ???
  }
  case class FinalBlockId() extends NDNTLVType {
    val typ = 26
  }
  //  Data/Signature

  object SignatureType extends NDNTLVTypeBuilder[SignatureType] {
    override def from(tlv: TLV): SignatureType = ???
  }
  case class SignatureType() extends NDNTLVType {
    val typ = 27
  }

  object KeyLocator extends NDNTLVTypeBuilder[KeyLocator] {
    override def from(tlv: TLV): KeyLocator = ???
  }
  case class KeyLocator() extends NDNTLVType with RecursingType {
    val typ = 28
  }
  object KeyDigest extends NDNTLVTypeBuilder[KeyDigest] {
    override def from(tlv: TLV): KeyDigest = ???
  }
  case class KeyDigest() extends NDNTLVType {
    val typ = 29
  }

//  case class Empty() extends NDNTLVType

  case class TLV(t: Int, l: Int, v: List[Byte])

  def error(msg: String) = {
    throw new Exception(msg)
  }

  def errorWrongType(typeName: String, actual: Int, expected: Int) = {
    error(s"$typeName has type $expected and not $actual")
  }

  def warning(msg: String) = {
    println(msg)
  }

  def decodeTLVList(data: List[Byte]): List[TLV] = {
    if(data.isEmpty) Nil
    else {
      val (tlv, tail) = decodeSingleTLV(data)
      tlv :: decodeTLVList(tail)
    }
  }

  def nonNegativeInteger(d: List[Byte]): Int = {
    println("warning, nonNegativeInteger not yet implemented")
    1
  }

  def varNumberToInt(d: List[Byte]): Int = {
    d match {
      case List(b) => b
      case List(b1, b2) => ByteBuffer.wrap(Array(0.toByte, 0.toByte, b1, b2)).getInt
      case List(b1, b2, b3, b4) => ByteBuffer.wrap(Array(b1, b2, b3, b3)).getInt
    }

  }

  def decodeSingleTLV(data: List[Byte]): (TLV, List[Byte]) = {

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
    val typ = varNumberToInt(typVN)
    val (lengthVN, dataV) = varNumber(dataLV)
    //      println(s"length: ${varNumberToInt(lengthVN)}")
    val length = varNumberToInt(lengthVN)
    val (value, data_) = {
      val (dataList, data_) = dataV.splitAt(length)
      if(dataList.size != length) error(s"data field not long enough: length=$length datafield='${new String(dataList.toArray)}'")
      (dataList, data_)
    }

    (TLV(typ, length, value), data_)
  }
//  def typToT(typ: Int): NDNTLVType = {
//    typ match {
//      case 5 => Interest()
//      case 6 => Data()
//      case 7 => Name()
//      case 8 => NameComponent()
//      case 9 => Selectors()
//      case 10 => Nonce()
//      case 11 => Scope()
//      case 12 => InterestLifetime()
//      case 13 => MinSuffixComponents()
//      case 14 => MaxSuffixComponents()
//      case 15 => PublisherPublicKeyLocator()
//      case 16 => Exclude()
//      case 17 => ChildSelector()
//      case 18 => MustBeFresh()
//      case 19 => Any()
//      case 20 => MetaInfo()
//      case 21 => Content()
//      case 22 => SignatureInfo()
//      case 23 => SignatureValue()
//      case 24 => ContentType()
//      case 25 => FreshnessPeriod()
//      case 26 => FinalBlockId()
//      case 27 => SignatureType()
//      case 28 => KeyLocator()
//      case 29 => KeyDigest()
//      case _ => throw new Exception(s"unkown type $typ")
//    }
//  }

//  def decode(dataArr: Seq[Byte], lvl: Int): Unit = {
//
//
//    def printlnLvl(msg: String, l: Int) = println(" "*l*2 + msg)
//
//    if(dataArr.size == 0) return
//
//    val (tlv, tail) = decodeSingleTLV(dataArr.toList)
//
//    printlnLvl(tlv.t.toString, lvl)
//    if(tlv.t.isInstanceOf[RecursingType]) {
//      decode(tlv.v, lvl + 1)
//    } else {
//      val a = tlv.v.toArray
//      printlnLvl(s"'${new String(a)}' (s=${a.size})", lvl + 1)
//    }
//
//    decode(tail, lvl)
//  }
}

object TLVTest extends App {
  val ba = IOHelper.readByteArrayFromFile("./asdf_asdf.ndn")

  println(s"ba (s=${ba.size})")
  println(PacketType.from(ba.toList))
//  TLVDecoder.decode(ba, 0)
}

