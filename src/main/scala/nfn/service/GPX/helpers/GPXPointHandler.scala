package nfn.service.GPX.helpers

import ccn.packet.{Interest, Content, CCNName}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
object GPXPointHandler {

  type GPXPoint = (Double, Double, String)

  def parseGPXPoint(inputdata: Content) : GPXPoint = {

    val input = new String(inputdata.data.map(_.toChar));
    val xml = scala.xml.XML.loadString(input)

    val latitude = xml  \ "@lat"
    val longitude = xml \ "@lon"
    val time =  xml \ "time"

    (latitude.text.toDouble, longitude.text.toDouble, time.text)
  }

  def createXmlGPXPoint(p: GPXPoint): String = {
    <trkpt lat={p._1.toString} lon={p._2.toString}>
      <time>{p._3}</time>
    </trkpt>.toString()
  }

}
