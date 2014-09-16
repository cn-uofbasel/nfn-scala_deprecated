package ccn.ccnlite

import ccn.NFNCCNLiteParser
import ccn.ccnlite.ndntlv.ccnlitecontentformat._
import ccn.packet._
import java.io.{FileOutputStream, File}
import ccnliteinterface._
import com.typesafe.scalalogging.slf4j.Logging
import myutil.FormattedOutput

import scala.collection.mutable.ListBuffer


object CCNLiteInterfaceWrapper {

  def createCCNLiteInterfaceWrapper(wireFormat: CCNLiteWireFormat, ccnIfType: CCNLiteInterfaceType) =
    CCNLiteInterfaceWrapper(CCNLiteInterface.createCCNLiteInterface(wireFormat, ccnIfType))
}

/**
 * Wrapper for the [[CCNLiteInterface]]
 */
case class CCNLiteInterfaceWrapper(ccnIf: CCNLiteInterface) extends Logging {

  def ccnbToXml(ccnbData: Array[Byte]): String = {
    // This synchronized is required because currently ccnbToXml writes to the local file c_xml.txt
    // This results in issues when concurrenlty writing/creating/deleting the same file (filelock)
    // Fix the implementation of Java_ccnliteinterface_CCNLiteInterface_ccnbToXml in the ccnliteinterface and remove synchronized
    CCNLiteInterfaceWrapper.synchronized {
      ccnIf.ccnbToXml(ccnbData)
    }
  }

  def mkBinaryContent(content: Content): List[Array[Byte]] = {

    val maxSegmentSize = 100

    val segmentSize = maxSegmentSize

    val dataSize = content.data.size
    val segmentComponent = "s"

    def singleContentData(data: Array[Byte]): Array[Byte] = {
      SingleContent(None, None, Data(data.toList)).encodeData
    }

    content match {
      case Content(name, data) if dataSize > segmentSize => {
        val buf = ListBuffer[Array[Byte]]()
        def go(segNum: Int, largeData: Array[Byte]): List[Array[Byte]] = {
          val (segData, largeDataTail) = largeData.splitAt(segmentSize)

          val segName = CCNName(content.name.cmps.init ++ Seq(content.name.cmps.last + segmentComponent + segNum.toString):_*)
          buf.prepend(ccnIf.mkBinaryContent(segName.cmps.toArray, singleContentData(segData)))
          if (largeDataTail.nonEmpty) {
            go(segNum + 1, largeDataTail)
          } else {
            buf.toList
          }
        }

        val numberOfSegments = math.round(dataSize.toFloat / segmentSize + 0.5f)
        val lastSegmentSize = dataSize % segmentSize
        val multiSegmentContent = MultiSegmentContent(None, None, Some(SegmentName(segmentComponent)), NumberOfSegments(numberOfSegments), SegmentSize(segmentSize), LastSegmentSize(lastSegmentSize))
        val metaContentData =  multiSegmentContent.encodeData

        logger.info(s"Segmenting Content $content with meta information: $multiSegmentContent")
        if(metaContentData.size > segmentSize) throw new Exception(s"MetaData is too large to fit into a segment of size $segmentSize")
        ccnIf.mkBinaryContent(content.name.cmps.toArray, metaContentData) :: go(0, data)
      }
      case Content(name, data) => {
        ccnIf.mkBinaryContent(name.cmps.toArray, singleContentData(data)) :: Nil
      }
    }
  }


  def mkBinaryInterest(interest: Interest): Array[Byte] = {
    ccnIf.mkBinaryInterest(interest.name.cmps.toArray)
  }

  def mkBinaryPacket(packet: CCNPacket): List[Array[Byte]] = {
    packet match {
      case i: Interest => mkBinaryInterest(i) :: Nil
      case c: Content => mkBinaryContent(c)
      case n: NAck => mkBinaryContent(n.toContent)
    }
  }

  def mkBinaryInterest(name: Array[String]): Array[Byte] = {
    ccnIf.mkBinaryInterest(name)
  }



  def mkAddToCacheInterest(content: Content): List[Array[Byte]] = {

    // TODO: content format

    // TODO this is required because potentially several fies try to write to the same file, eve if it is very unlikely...
    // no longer required when addToCache does no longer require to parse a file or is implemented directly in Scala
    CCNLiteInterfaceWrapper.synchronized {
      mkBinaryContent(content) map { binaryContent =>

        val servLibDir = new File("./service-library")
        if (!servLibDir.exists) {
          servLibDir.mkdir()
        }
        val filename = s"./service-library/${content.name.hashCode}-${System.nanoTime}.ccnb"
        val file = new File(filename)
        file.delete()

        // Just to be sure, if the file already exists, wait quickly and try again
        file.createNewFile
        val out = new FileOutputStream(file)
        try {
          out.write(binaryContent)
        } finally {
          if (out != null) out.close
        }
        val absoluteFilename = file.getCanonicalPath
        val binaryInterest: Array[Byte] = ccnIf.mkAddToCacheInterest(absoluteFilename)

        file.delete

        binaryInterest
      }
    }
  }


  def base64CCNBToPacket(base64ccnb: String): Option[CCNPacket] = {
    val xml = ccnIf.ccnbToXml(NFNCCNLiteParser.decodeBase64(base64ccnb))
    val pkt = NFNCCNLiteParser.parseCCNPacket(xml)
    pkt
  }

  private def mkAddToCacheInterest(ccnbAbsoluteFilename: String): Array[Byte] = {
    ccnIf.mkAddToCacheInterest(ccnbAbsoluteFilename)
  }

  def byteStringToPacket(byteArr: Array[Byte]): Option[Packet] = {
    NFNCCNLiteParser.parseCCNPacket(ccnbToXml(byteArr))
  }
}
