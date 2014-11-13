package ccn.ccnlite

import javax.xml.bind.DatatypeConverter

import ccn.packet._
import com.typesafe.scalalogging.slf4j.Logging
import org.xml.sax.SAXParseException

import scala.util.{Failure, Success, Try}
import scala.xml._


object CCNLiteXmlParser extends Logging {

  def decodeBase64(data: String): Array[Byte] = DatatypeConverter.parseBase64Binary(data)
  def encodeBase64(bytes: Array[Byte]): String = DatatypeConverter.printBase64Binary(bytes)


  def parseCCNPacket(xmlString: String):Try[CCNPacket] = {


    def parseComponentsCCNB(elem: Elem):Seq[String] = {
      val components = elem \ "name" \ "component"

      components.map { parseDataCCNB }
    }

    def parseComponentsNDNTLV(elem: Elem):Seq[Array[Byte]] = {
      val components = elem \ "Name" \ "NameComponent"

      val cmps = components.map { parseDataNDNTLV }

      cmps
    }
    def parseDataCCNB(elem: Node): String = {

      val data = elem \ "data"

      val nameSize = (data \ "@size").text.toInt
      val encoding = (data \ "@dt").text
      val nameData = data.text.trim

      encoding match {
        case "string" =>
          nameData
        case "binary.base64" =>
          new String(decodeBase64(nameData))
        case _ => throw new Exception(s"parseDataCCNB() does not support data of type: '$encoding'")
      }
    }

    def parseDataNDNTLV(elem: Node): Array[Byte] = {
      val datasNodes = elem \ "data"

      datasNodes.foldLeft(Array[Byte]()) { (curData: Array[Byte], data: Node) =>
        val nameSize = (data \ "@size").text.toInt
        val encoding = (data \ "@dt").text
        val nameData = data.text.trim

        val nextData = encoding match {
          case "string" =>
            nameData.getBytes
          case "binary.base64" =>
            decodeBase64(nameData)
          case _ => throw new Exception(s"parseDataCCNB() does not support data of type: '$encoding'")
        }

        curData ++ nextData
      }
    }

    def parseContentDataCCNB(elem: Elem): Array[Byte] = {
      val contents = elem \ "content"

      assert(contents.size == 1, "content should only contain one node with content")
      parseDataCCNB(contents.head).getBytes
    }
    def parseContentDataNDNTLV(elem: Elem): Array[Byte] = {
      val contents = elem \ "Content"

      assert(contents.size == 1, "content should only contain one node with content")
      parseDataNDNTLV(contents.head)
    }


    val cleanedXmlString = xmlString.trim.replace("&", "&amp;")
    val addToCacheAckStringBase64 = "MCvi6qVsYXN0AAGqfsXy+qVjY254APqFAPr1YWRkY2FjaGVvYmplY3QAAAGaAf0EygHVQ29udGVudCBzdWNjZXNzZnVsbHkgYWRkZWQ"
    val addToCacheAckStringBase642 ="8vqlY2NueAD6hQD69WFkZGNhY2hlb2JqZWN0AAABmgH9BMoB1UNvbnRlbnQgc3VjY2Vzc2Z1bGx5IGFkZGVk"
    //        val addToCacheNackStringBase64 = "8vqlY2NueAD6hQD69WFkZGNhY2hlb2JqZWN0AAABmgH9BMoB1UNvbnRlbnQgc3VjY2Vzc2Z1bGx5IGFkZGVk"
    val addToCacheFailedStringBase64 = "8vqlY2NueAD6hQD69WFkZGNhY2hlb2JqZWN0AAABmgHVBMoBrUZhaWxlZCB0byBhZGQgY29udGVud"
    if(cleanedXmlString.contains(addToCacheAckStringBase64) || cleanedXmlString.contains(addToCacheAckStringBase642)) {
      Try(AddToCacheAck(CCNName("/")))
    } else if (cleanedXmlString.contains(addToCacheFailedStringBase64)) {
      Try(AddToCacheNack(CCNName("/")))
    } else {

      val triedParsePacket =
      Try {
        scala.xml.XML.loadString(cleanedXmlString) match {
          // CCNB interest
          case interest @ <interest>{_*}</interest>=> {
            val nameComponents = parseComponentsCCNB(interest)
            Interest(nameComponents :_*)
          }
          // CCNB content
          case content @ <contentobj>{_*}</contentobj> => {
            val nameComponents = parseComponentsCCNB(content)
            val contentData = parseContentDataCCNB(content)
            if(contentData.startsWith(":NACK".getBytes)) {
              Nack(CCNName(nameComponents :_*))
            } else {
              Content(contentData, nameComponents :_*)
            }
          }

          // CCNTLV and NDNTLV both are pktdumped with <Interest><Name>
          // they differ by NameSegments and NameComponents
          case interest @ <Interest>{_*}</Interest> => {
            val name: NodeSeq = interest \ "Name"

            val nameSegments  = name \ "NameSegment"
            val nameComponents =
              if(nameSegments.nonEmpty) {
                nameSegments map { ns => new String(decodeBase64(ns.text.trim)) }
              }
              else {
                parseComponentsNDNTLV(interest) map { cmp => new String(cmp) }
              }
            Interest(nameComponents :_*)
          }
          /*
            <Object>
              <Name>
                <NameSegment size="4" dt="binary.base64">
                  Y2NueA==
                </NameSegment>
                <NameSegment size="6" dt="binary.base64">
                  c2ltcGxl
                </NameSegment>
              </Name>
              <Payload size="12" dt="binary.base64">
               dGVzdGNvbnRlbnQK
              </Payload>
            </Object>
           */
          case content @ <Object>{_*}</Object> => {
            val nameComponents = content \ "Name" \ "NameSegment" map { ns => new String(decodeBase64(ns.text.trim)) }
            val contentData = content \ "Payload" map {d => decodeBase64(d.text.trim) } reduceLeft(_ ++ _)
            if(contentData.startsWith(":NACK".getBytes)) {
              Nack(CCNName(nameComponents :_*))
            } else {
              Content(contentData, nameComponents :_*)
            }
          }

          case content @ <Data>{_*}</Data> => {
            val nameComponents = parseComponentsNDNTLV(content) map { new String(_) }
            val contentData = parseContentDataNDNTLV(content)
            if(contentData.startsWith(":NACK".getBytes)) {
              Nack(CCNName(nameComponents :_*))
            } else {
              Content(contentData, nameComponents :_*)
            }
          }
          case _ => throw new Exception("XML parser cannot parse:\n" + cleanedXmlString)
        }
      }
      // transform exception to print the original xml
      triedParsePacket.transform (
        s => Success(s),
        e => Failure( new Exception(s"Could not parse $cleanedXmlString",e) )
      )
    }
  }
}
