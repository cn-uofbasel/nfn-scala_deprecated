package ccn.ccnlite.ndntlv

import ccn.ccnlite.ndntlv.tlvtranscoding.{TLVDecoder, TLV}
import myutil.IOHelper

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{Position, Reader}

object CCNParser extends TLVParser {
  override def grammar: Parser[NDNTLVType] = interestPacket | dataPacket
  def interestPacket: Parser[Interest] = typ(5) ^^ { case tlvInterest => parse(tlvInterest.v, interest) }
  def dataPacket: Parser[Data] = typ(6) ^^ { case tlvData => parse(tlvData.v, data) }

  def data: Parser[Data] = ???
  def interest: Parser[Interest] =  typ(7) ~ typ(9).? ~ dataType(10) ~ dataType(11).? ~ dataType(12).? ^^ { case nameT ~ selectorsT ~ nonceT ~ scopeT ~ interestLifetimeT=>
    Interest(parse(nameT.v, name),
      selectorsT map {sel => parse(sel.v, selectors)},
      Nonce(nonceT),
      scopeT map { s => Scope(nonNegativeInteger(s.toList))},
      interestLifetimeT map {ilt => InterestLifetime(nonNegativeInteger(ilt.toList)) }
    )}

  def name: Parser[Name] = dataType(8).* ^^ {case nameComponents => Name(nameComponents map { nc => NameComponent(new String(nc.toArray))})}

  def selectors: Parser[Selectors] = dataType(13).? ~ dataType(14).? ~ dataType(15).? ~ dataType(16).? ~ dataType(17).? ~ dataType(18).? ~ dataType(19).? ^^ {case minsc ~ maxsc ~ ppkl ~ exclude ~ cs ~ mbf ~ any =>
    Selectors(
      minsc map {minsc => MinSuffixComponents(nonNegativeInteger(minsc))},
      maxsc map {maxsc => MaxSuffixComponents(nonNegativeInteger(maxsc))},
      ppkl map {ppkl => PublisherPublicKeyLocator()},
      exclude map {exlucde => Exclude()},
      cs map {_ => ChildSelector()},
      mbf map { _ => MustBeFresh()},
      any map {_ => Any()}
    )}
}

trait TLVParser extends Parsers {

  private class TLVStreamReader(tlvs: List[TLV], elemNum: Int = 0) extends Reader[TLV] {

    override def first = tlvs.headOption.getOrElse(TLV(-1, 0, Nil))

    override def atEnd: Boolean = tlvs.size <= 0

    private class TLVPosition(tlv: TLV) extends Position {
      override def line: Int = 1

      override def column: Int = elemNum

      override protected def lineContents: String = tlv.toString
    }

    override def rest: TLVStreamReader = {

      val v = if(tlvs.size > 0) tlvs.tail else Nil
      new TLVStreamReader(v, elemNum + 1)
    }

    override def pos: Position = new TLVPosition(if(tlvs.size > 0) tlvs.head else TLV(-1, 0, Nil))
  }

  override type Elem = TLV

  def parse(data: List[Byte]): ParseResult[NDNTLVType] = {
    val tlvs = TLVDecoder.decodeTLVs(data)
    val reader = new TLVStreamReader(tlvs)
    phrase(grammar)(reader)
  }

  def parse[A](data: List[Byte], f: Parser[A]): A= {
    val tlvs = TLVDecoder.decodeTLVs(data)
    val reader = new TLVStreamReader(tlvs)
    phrase(f)(new TLVStreamReader(tlvs)).get
  }

  def typ(n: Int): Parser[TLV] = Parser { f =>
    f.first match {
      case tlv if tlv.t == n =>  Success(f.first, f.rest)
      case _ => Failure(s"type: $n", f.rest)
    }
  }

  def dataType(n: Int): Parser[List[Byte]] = Parser { f =>
    f.first match {
      case tlv if tlv.t == n =>  Success(tlv.v, f.rest)
      case _ => Failure(s"datatype: $n", f.rest)
    }
  }

  def grammar: Parser[NDNTLVType]

  def nonNegativeInteger(data: Array[Byte]): Long = nonNegativeInteger(data.toList)
  def nonNegativeInteger(data: List[Byte]): Long = TLVDecoder.nonNegativeInteger(data)

}
