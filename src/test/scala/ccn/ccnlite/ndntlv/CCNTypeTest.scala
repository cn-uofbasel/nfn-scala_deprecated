package ccn.ccnlite.ndntlv

import ccn.ccnlite.ndntlv.tlvtranscoding.TLVEncoder
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by basil on 12/09/14.
 */
class CCNTypeTest extends FlatSpec with Matchers {

  "A simple ndn interterest" should "be both encoded and decoded correctly" in {
    // NDNTLV interest for name /asdf/asdf with nonce, Selector.mustbefresh and an interset lifetime of 4000
    val encodedOriginal = List(5, 28, 7, 12, 8, 4, 97, 115, 100, 102, 8, 4, 97, 115, 100, 102, 9, 2, 18, 0, 10, 4, -100, 114, 16, 84, 12, 2, 15, -96) map { _.toByte }
    val originalInterest = Interest(Name(List(NameComponent("asdf"), NameComponent("asdf"))), Some(Selectors(None, None, None, None, None, Some(MustBeFresh()), None)), Nonce(List(0x9c.toByte, 0x72.toByte, 0x10.toByte, 0x54.toByte)), None, Some(InterestLifetime(4000)))
    val interest = CCNParser.parse(encodedOriginal).get


    val encoded = TLVEncoder.encode(interest.encode)
    val interest2 = CCNParser.parse(encoded).get

    interest shouldBe originalInterest
    interest shouldBe interest2
    encodedOriginal shouldBe encoded
  }


}
