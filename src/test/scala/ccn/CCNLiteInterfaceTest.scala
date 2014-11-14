package ccn

import ccn.ccnlite.CCNLiteInterfaceCli
import ccn.packet._
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global


class CCNLiteInterfaceTest extends FlatSpec with Matchers with GivenWhenThen {

  testCCNLiteInterfaceWrapper(CCNLiteInterfaceCli(CCNBWireFormat()))
  testCCNLiteInterfaceWrapper(CCNLiteInterfaceCli(NDNTLVWireFormat()))
  testCCNLiteInterfaceWrapper(CCNLiteInterfaceCli(CCNTLVWireFormat()))
  def testCCNLiteInterfaceWrapper(ccnIf: CCNInterface) = {

    val testInterest = Interest("name", "testInterest")

    s"CCNLiteInterface of type $ccnIf with wire format ${ccnIf.wireFormat} with test interest $testInterest" should "be converted to wire format back to test interest object" in {
      for {
        wirePacket <- ccnIf.mkBinaryInterest(testInterest)
        resultPacket <- ccnIf.wireFormatDataToXmlPacket(wirePacket)
      } yield {
        resultPacket should be (a [Interest])
        resultPacket.name should be (testInterest.name)
      }
    }

    val content:Content = Content("testcontent".getBytes, "name", "content")

    s"CCNLiteInterface of type $ccnIf with wire format ${ccnIf.wireFormat} with content $content" should "be converted to wire format back to content object" in {

      ccnIf.mkBinaryContent(content) foreach { binaryContents =>
        binaryContents.size should be(1)
        val binaryContent = binaryContents.head
        ccnIf.wireFormatDataToXmlPacket(binaryContent) foreach { resultPacket =>
          resultPacket should be(a[Content])
          resultPacket.name should be(content.name)
          resultPacket.asInstanceOf[Content].data shouldBe "testcontent".getBytes
        }

      }
    }
    s"CCNLiteInterface of type $ccnIf with wire format ${ccnIf.wireFormat} with Content $content" should "be converted to (ccnb) wire format for an addToCache request" in {
      ccnIf.mkAddToCacheInterest(content) foreach { wireFormatAddToCacheReqs =>
        wireFormatAddToCacheReqs.size should be(1)
        val wireFormatAddToCacheReq = wireFormatAddToCacheReqs.head
        new String(wireFormatAddToCacheReq).contains("testdata") shouldBe true
      }
    }
  }

}
