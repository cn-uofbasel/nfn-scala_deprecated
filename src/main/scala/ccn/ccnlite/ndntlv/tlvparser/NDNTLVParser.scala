package ccn.ccnlite.ndntlv.tlvparser

import ccn.ccnlite.ndntlv.NDNTLVType.NDNTLVException
import ccn.ccnlite.ndntlv.tlvparser.NDNTLVParser.NDNTLVParseException
import ccn.ccnlite.ndntlv.{NonNegativeIntegerType, NDNTLVType}
import ccn.ccnlite.ndntlv.tlvtranscoding.{TLV, TLVDecoder}

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{Position, Reader}


object NDNTLVParser {
  class NDNTLVParseException(msg: String) extends NDNTLVException(msg)
}

trait NDNTLVParser extends Parsers {

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

  def parse(data: List[Byte]): NDNTLVType = {
    val tlvs = TLVDecoder.decodeTLVs(data)
    val reader = new TLVStreamReader(tlvs)
    phrase(grammar)(reader) match {
      case Success(v, _) => v
      case Failure(eMsg, _) => throw new NDNTLVParseException(s"Parse error: $eMsg")
    }
  }

  def parse[A](data: List[Byte], f: Parser[A]): A = {
    val tlvs = TLVDecoder.decodeTLVs(data)
    val reader = new TLVStreamReader(tlvs)
    phrase(f)(reader) match {
      case Success(v, _) => v
      case Failure(eMsg, _) => throw new NDNTLVParseException(s"error when parsing tlvs: '$tlvs': $eMsg")
    }
  }

  def verifyTyp[A](n: Int)(f: TLV => A): Parser[A] = Parser { parser =>
    parser.first match {
      case tlv if tlv.t == n => Success(f(parser.first), parser.rest)
      case _ => Failure(s"type: $n", parser.rest)
    }
  }

  def typ(n: Int): Parser[TLV] = verifyTyp(n)(tlv => tlv)

  def dataType(n: Int): Parser[List[Byte]] = verifyTyp(n) (_.v)

  def nonNegInType(n: Int): Parser[Long] =  verifyTyp(n)(tlv => TLVDecoder.nonNegativeInteger(tlv.v))

  def stringType(n: Int): Parser[String] = verifyTyp(n)(tlv => {
    new String(tlv.v.toArray)
  })

  def grammar: Parser[NDNTLVType]

  def nonNegativeInteger(data: Array[Byte]): Long = nonNegativeInteger(data.toList)
  def nonNegativeInteger(data: List[Byte]): Long = TLVDecoder.nonNegativeInteger(data)

}
