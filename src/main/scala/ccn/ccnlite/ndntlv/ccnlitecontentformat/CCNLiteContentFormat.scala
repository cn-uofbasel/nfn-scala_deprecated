package ccn.ccnlite.ndntlv.ccnlitecontentformat

import ccn.ccnlite.ndntlv._
import ccn.ccnlite.ndntlv.ndnpacketformat.ContentType
import ccn.ccnlite.ndntlv.tlvparser.NDNTLVParser
import ccn.ccnlite.ndntlv.tlvtranscoding.{TLVDecoder, TLVEncoder, TLV}


object CCNLiteContentFormatParser extends NDNTLVParser {
  override def grammar: Parser[ContentType] = singleContentType | multiSegmentContentType | streamContentType

  def singleContentType: Parser[ContentType] = dataType(40000) ^^ { case singleContentData => parse(singleContentData, singleContent) }

  def multiSegmentContentType: Parser[ContentType] = dataType(40001) ^^ { case data =>  parse(data, multiSegmentContent) }

  def streamContentType: Parser[ContentType] = dataType(40002) ^^ { case singleContentData => parse(singleContentData, streamContent) }

  def singleContent: Parser[SingleContent] = stringType(40010).? ~ stringType(40011).? ~ dataType(40012) ^^
    { case mime ~ charSet ~ data => SingleContent(mime map MimeType, charSet map CharSet, Data(data))}

  def multiSegmentContent: Parser[MultiSegmentContent] = stringType(40010).? ~ stringType(40011).? ~ stringType(40020).? ~ nonNegInType(40021) ~ nonNegInType(40022) ~ nonNegInType(40023) ^^
    { case mimeType ~ charSet ~ segName ~ numOfSegs ~ segSize ~ lastSegSize => MultiSegmentContent(mimeType map MimeType, charSet map CharSet, segName map SegmentName, NumberOfSegments(numOfSegs), SegmentSize(segSize), LastSegmentSize(lastSegSize))}
  def streamContent: Parser[StreamContent] = stringType(40010).? ~ stringType(40011).? ~ stringType(40020).? ~ nonNegInType(40022) ~ nonNegInType(40030).? ^^
    { case mimetype ~ charSet ~ segName ~ segSize ~ bitrate => StreamContent(mimetype map MimeType, charSet map CharSet, segName map SegmentName, SegmentSize(segSize), bitrate map AverageApproximatedBitRate)}
}


trait ContentType extends RecursiveType


case class SingleContent(mimeType: Option[MimeType], charSet: Option[CharSet], data: Data) extends ContentType {
  override def typ: Int = 40000

  override def unapply: List[Option[NDNTLVType]] = List(mimeType, charSet, Some(data))
}


case class MultiSegmentContent(mimeType: Option[MimeType],
                               charSet: Option[CharSet],
                               segmentName: Option[SegmentName],
                               numOfSegments: NumberOfSegments,
                               segmentSize: SegmentSize,
                               lastSegmentSize: LastSegmentSize ) extends ContentType {

  override def typ: Int = 40001

  override def unapply: List[Option[NDNTLVType]] = List(mimeType, charSet, segmentName,  Some(numOfSegments), Some(segmentSize), Some(lastSegmentSize))
}

case class StreamContent(mimeType: Option[MimeType], charSet: Option[CharSet], segmentName: Option[SegmentName], segmentSize: SegmentSize, bitrate: Option[AverageApproximatedBitRate]) extends ContentType {
  override def typ: Int = 40002


  override def unapply: List[Option[NDNTLVType]] = List(mimeType, charSet, segmentName, Some(segmentSize), bitrate)
}


trait GeneralContentInfo extends NDNTLVType

case class MimeType(str: String) extends GeneralContentInfo with StringType {
  override def typ: Int = 40010
}

case class CharSet(str: String) extends  GeneralContentInfo with StringType {
  override def typ: Int = 40011
}

case class Data(data: List[Byte]) extends GeneralContentInfo with DataType {
  override def typ: Int = 40012
}


trait MultiSegmentInfo extends NDNTLVType

case class SegmentName(str: String) extends MultiSegmentInfo with StringType {
  override def typ: Int = 40020
}

case class NumberOfSegments(nonNegInt: Long) extends MultiSegmentInfo with NonNegativeIntegerType {
  override def typ: Int = 40021

}

case class SegmentSize(nonNegInt: Long) extends MultiSegmentInfo with NonNegativeIntegerType {
  override def typ: Int = 40022
}


case class LastSegmentSize(nonNegInt: Long) extends MultiSegmentInfo with NonNegativeIntegerType {
  override def typ: Int = 40023
}

trait StreamMetaInformation extends NDNTLVType

case class AverageApproximatedBitRate(nonNegInt: Long) extends NDNTLVType with NonNegativeIntegerType {
  override def typ: Int = 40030
}

