package ccn.ccnlite.ndntlv

import ccn.ccnlite.ndntlv.tlvtranscoding.{TLVEncoder, TLV, TLVDecoder}

trait NDNTLVType {
  def typ: Int
  def encode:TLV
}

trait EmptyType extends NDNTLVType {
  override def encode: TLV = {
    TLV(typ, 0, Nil)
  }
}

trait DataType extends NDNTLVType {
  override def encode: TLV = {
    TLV(typ, data.size, data.toList)
  }

  def data: List[Byte]

}

trait StringType extends NDNTLVType {
  override def encode: TLV = {
    val data = str.getBytes.toList
    TLV(typ, data.size, data)
  }

  def str: String
}

trait NonNegativeIntegerType extends NDNTLVType {
  override def encode: TLV = {
    val data = TLVEncoder.longToNonNegativeInteger(nonNegInt)

    TLV(typ, data.size, data)
  }

  def nonNegInt: Long
}

trait PacketType extends NDNTLVType

case class Interest(name: Name, selectors: Option[Selectors], nonce: Nonce, scope: Option[Scope], interestLifetime: Option[InterestLifetime]) extends NDNTLVType with PacketType {
  val typ = 5

  override def encode: TLV = {
    val tlvs: List[Option[TLV]] = List(Some(name.encode), selectors map {_.encode}, Some(nonce.encode), scope map {_.encode}, interestLifetime map {_.encode})
    val data: List[Byte] = TLVEncoder.encode(tlvs)
    TLV(typ, data.length, data)
  }
}
case class Data() extends NDNTLVType with PacketType with EmptyType {
  val typ = 6
}

//  Common fields

case class Name(nameComps: List[NameComponent]) extends NDNTLVType {
  val typ = 7

  override def encode: TLV = {
    val tlvs = nameComps map {nc => Some(nc.encode)}
    val data = TLVEncoder.encode(tlvs)
    TLV(typ, data.length, data)
  }
}

case class NameComponent(str: String) extends NDNTLVType with StringType {
  val typ = 8
}

//  Interest packet

case class Selectors(
                      minSuffixComponents: Option[MinSuffixComponents],
                      maxSuffixComponents: Option[MaxSuffixComponents],
                      publisherPublicKeyLocator: Option[PublisherPublicKeyLocator],
                      exclude: Option[Exclude],
                      childSelector: Option[ChildSelector],
                      mustBeFresh: Option[MustBeFresh],
                      any: Option[Any]) extends NDNTLVType {
  val typ = 9

  override def encode: TLV = {
    val tlvs: List[Option[TLV]] = List(minSuffixComponents map {_.encode},
                                       maxSuffixComponents map {_.encode},
                                       publisherPublicKeyLocator map {_.encode},
                                       exclude map {_.encode},
                                       childSelector map {_.encode},
                                       mustBeFresh map {_.encode}
                                      )
    val data = TLVEncoder.encode(tlvs)
    TLV(typ, data.length, data)
  }
}

case class Nonce(data: List[Byte]) extends NDNTLVType with DataType {
  val typ = 10
}

case class Scope(nonNegInt: Long) extends NDNTLVType with NonNegativeIntegerType {
  val typ = 11
}

case class InterestLifetime(nonNegInt: Long) extends NDNTLVType with NonNegativeIntegerType {
  val typ = 12
}

trait Selector

//  Interest/Selectors

case class MinSuffixComponents(nonNegInt: Long) extends NDNTLVType with Selector with NonNegativeIntegerType {
  val typ = 13
}

case class MaxSuffixComponents(nonNegInt: Long) extends NDNTLVType with Selector with NonNegativeIntegerType{
  val typ = 14
}

case class PublisherPublicKeyLocator(/*TODO: KeyLocator*/) extends NDNTLVType with Selector with EmptyType {
  val typ = 15
}

case class Exclude() extends NDNTLVType with Selector with EmptyType {
  val typ = 16
}

case class ChildSelector() extends NDNTLVType with Selector with EmptyType {
  val typ = 17
}

case class MustBeFresh() extends NDNTLVType with Selector with EmptyType {
  val typ = 18
}

case class Any() extends NDNTLVType with Selector with EmptyType {
  val typ = 19
}

//  Data packet

case class MetaInfo() extends NDNTLVType with EmptyType {
  val typ = 20
}

case class Content() extends NDNTLVType with EmptyType {
  val typ = 21
}

case class SignatureInfo() extends NDNTLVType with EmptyType {
  val typ = 22
}

case class SignatureValue() extends NDNTLVType with EmptyType {
  val typ = 23
}

//  Data/MetaInfo

case class ContentType() extends NDNTLVType with EmptyType {
  val typ = 24
}

case class FreshnessPeriod() extends NDNTLVType with EmptyType {
  val typ = 25
}

case class FinalBlockId() extends NDNTLVType with EmptyType {
  val typ = 26
}

//  Data/Signature

case class SignatureType() extends NDNTLVType with EmptyType {
  val typ = 27
}

case class KeyLocator() extends NDNTLVType with EmptyType {
  val typ = 28
}

case class KeyDigest() extends NDNTLVType with EmptyType {
  val typ = 29
}

object TLVTest extends App {
  // NDNTLV interest for name /asdf/asdf with nonce, Selector.mustbefresh and an interset lifetime of 4000
  val ba = List(5, 28, 7, 12, 8, 4, 97, 115, 100, 102, 8, 4, 97, 115, 100, 102, 9, 2, 18, 0, 10, 4, -100, 114, 16, 84, 12, 2, 15, -96) map { _.toByte }

  Array(ba)
  println(s"ba (s=${ba.size})")
//  val (r, _) =
  val r = CCNParser.parse(ba).get
  println(s"parsed: $r")

  val encoded = TLVEncoder.encode(r.encode)
  println(s"encoded: size=${encoded.size} '$encoded'")
  val decodedEncoded = CCNParser.parse(encoded)
  println(s"decodedencoded: $decodedEncoded")

}

