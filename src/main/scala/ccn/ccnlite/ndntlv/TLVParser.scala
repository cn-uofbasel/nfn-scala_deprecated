//package ccn.ccnlite.ndntlv
//
//import ccn.ccnlite.ndntlv.TLVDecoder.TLV
//
//import scala.util.parsing.combinator.Parsers
//import scala.util.parsing.input.{Position, Reader}
//
//
//class TLVParser extends Parsers {
//
//  private class TLVStreamReader(data: List[Byte], elemNum: Int = 0) extends Reader[TLV] {
//
//    lazy val (tlv, tail) = TLVDecoder.decodeSingleTLV(data)
//
//    override def first: TLV = tlv
//
//    override def atEnd: Boolean = tail.isEmpty
//
//    private class TLVPosition(tlv: TLV) extends Position {
//      override def line: Int = 1
//
//      override def column: Int = elemNum
//
//      override protected def lineContents: String = tlv.toString
//    }
//
//    override def rest: Reader[TLV] = new TLVStreamReader(tail)
//
//    override def pos: Position = new TLVPosition(tlv)
//  }
//
//  override type Elem = TLV
//
//
//}
