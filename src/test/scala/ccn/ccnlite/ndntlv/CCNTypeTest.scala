package ccn.ccnlite.ndntlv

//import ccn.ccnlite.ndntlv.ccnlitecontentformat._

import ccn.ccnlite.ndntlv.ccnlitecontentformat._
import ccn.ccnlite.ndntlv.ndnpacketformat._
import ccn.ccnlite.ndntlv.tlvtranscoding._
import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}

/**
 * Created by basil on 12/09/14.
 */
class CCNTypeTest extends FlatSpec with Matchers with GivenWhenThen {

  "A simple ndn interterest" should "be both encoded and decoded correctly" in {
    // NDNTLV interest for name /asdf/asdf with nonce, Selector.mustbefresh and an interset lifetime of 4000
    val encodedOriginal = List(5, 28, 7, 12, 8, 4, 97, 115, 100, 102, 8, 4, 97, 115, 100, 102, 9, 2, 18, 0, 10, 4, -100, 114, 16, 84, 12, 2, 15, -96) map { _.toByte }
    val originalInterest = Interest(Name(List(NameComponent("asdf"), NameComponent("asdf"))), Some(Selectors(None, None, None, None, None, Some(MustBeFresh()), None)), Nonce(List(0x9c.toByte, 0x72.toByte, 0x10.toByte, 0x54.toByte)), None, Some(InterestLifetime(4000)))
    val interest = NDNPacketFormatParser.parse(encodedOriginal)


    val encoded = TLVEncoder.encode(interest.encode)
    val interest2 = NDNPacketFormatParser.parse(encoded)

    interest shouldBe originalInterest
    interest shouldBe interest2
    encodedOriginal shouldBe encoded
  }



  List(
    SingleContent(Some(MimeType("text")), Some(CharSet("UTF-8")), ccn.ccnlite.ndntlv.ccnlitecontentformat.Data("testdata".getBytes.toList)),
    MultiSegmentContent(Some(MimeType("text")), Some(CharSet("UTF-8")), Some(SegmentName("seg")), NumberOfSegments(2), SegmentSize(2000), LastSegmentSize(1000)),
    StreamContent(Some(MimeType("text")), Some(CharSet("UTF-8")), Some(SegmentName("seg")), SegmentSize(2), Some(AverageApproximatedBitRate(2)))
  ) map { ct => testCCNLiteContentFormat(ct, "with options") }

  List(
    SingleContent(None, None, ccn.ccnlite.ndntlv.ccnlitecontentformat.Data("testdata".getBytes.toList)),
    MultiSegmentContent(None, None, None, NumberOfSegments(2), SegmentSize(2000), LastSegmentSize(1000)),
    StreamContent(None, None, None, SegmentSize(2), None)
  ) map { ct => testCCNLiteContentFormat(ct, "without options") }

  def testCCNLiteContentFormat(c: ccnlitecontentformat.ContentType, testName: String) = {
    s"The ccn-lite content type $c for test $testName" should "be converted to NDNTLV and back to an object" in {
      //  println(TLVEncoder.longToVarNum(250))
      val tlvData = TLVEncoder.encode(c.encode)
      val c2 = CCNLiteContentFormatParser.parse(tlvData)

      c shouldBe c2
    }
  }


  val longNums = List(42L, 250L, 1000L, 40000L, 80000L)

  longNums map { testVarNum }

  def testVarNum(n: Long) = {
    s"The long number $n" should "be encoded to varnum and back to long" in {
      Given(s"number $n")

      val vn: List[Byte] = TLVEncoder.longToVarNum(n)
      When(s"encoded to $vn")

      val (decodedVN, tail) = TLVDecoder.decodeVarNumber(vn)
      val r = TLVDecoder.varNumberToLong(decodedVN)
      Then(s"Decoded to $r")

      r shouldBe n
      tail shouldBe (Nil)
    }
  }

  longNums map { testNonNegNum}
  def testNonNegNum(n: Long) = {
    s"The long number $n" should "be encoded to nonnegint and back to long" in {
      Given(s"number $n")

      val nni: List[Byte] = TLVEncoder.longToNonNegativeInteger(n)
      When(s"encoded to $nni")

      val r = TLVDecoder.nonNegativeInteger(nni)
      Then(s"Decoded to $r")

      r shouldBe n
    }
  }



}
