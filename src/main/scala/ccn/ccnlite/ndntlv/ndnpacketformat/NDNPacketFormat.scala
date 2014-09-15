package ccn.ccnlite.ndntlv.ndnpacketformat

import ccn.ccnlite.ndntlv._
import ccn.ccnlite.ndntlv.tlvparser._
import ccn.ccnlite.ndntlv.tlvtranscoding._

//import ccn.ccnlite.ndntlv.tlvtranscoding.{TLVEncoder, TLV}


object NDNPacketFormatParser extends NDNTLVParser {
  override def grammar: Parser[NDNTLVType] = interestPacket | dataPacket

  def interestPacket: Parser[Interest] = dataType(5) ^^ { case tlvInterest => parse(tlvInterest, interest) }

  def dataPacket: Parser[Data] = typ(6) ^^ { case tlvData => parse(tlvData.v, data) }

  def data: Parser[Data] = ???
  def interest: Parser[Interest] =  typ(7) ~ typ(9).? ~ dataType(10) ~ nonNegInType(11).? ~ nonNegInType(12).? ^^ { case nameT ~ selectorsT ~ nonceT ~ scopeT ~ interestLifetimeT=>
    Interest(parse(nameT.v, name),
      selectorsT map {sel => parse(sel.v, selectors)},
      Nonce(nonceT),
      scopeT map Scope ,
      interestLifetimeT map InterestLifetime
    )}

  def name: Parser[Name] = dataType(8).* ^^ {case nameComponents => Name(nameComponents map { nc => NameComponent(new String(nc.toArray))})}

  def selectors: Parser[Selectors] = nonNegInType(13).? ~ nonNegInType(14).? ~ dataType(15).? ~ dataType(16).? ~ dataType(17).? ~ dataType(18).? ~ dataType(19).? ^^ {case minsc ~ maxsc ~ ppkl ~ exclude ~ cs ~ mbf ~ any =>
    Selectors(
      minsc map MinSuffixComponents,
      maxsc map MaxSuffixComponents,
      ppkl map {ppkl => PublisherPublicKeyLocator()},
      exclude map {exlucde => Exclude()},
      cs map {_ => ChildSelector()},
      mbf map { _ => MustBeFresh()},
      any map {_ => Any()}
    )}
}


trait PacketType extends RecursiveType

case class Interest(name: Name, selectors: Option[Selectors], nonce: Nonce, scope: Option[Scope], interestLifetime: Option[InterestLifetime]) extends PacketType {
  val typ = 5

  override def unapply: List[Option[NDNTLVType]] = List(Some(name), selectors, Some(nonce), scope, interestLifetime)
}
case class Data() extends PacketType {
  val typ = 6

  override def unapply: List[Option[NDNTLVType]] = Nil
}

//  Common fields

case class Name(nameComps: List[NameComponent]) extends RecursiveType {
  val typ = 7

  override def unapply: List[Option[NDNTLVType]] = nameComps map {Some(_)}
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
                      any: Option[Any]) extends RecursiveType {
  val typ = 9

  override def unapply: List[Option[NDNTLVType]] = List( minSuffixComponents, maxSuffixComponents, publisherPublicKeyLocator, exclude, childSelector, mustBeFresh)
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


