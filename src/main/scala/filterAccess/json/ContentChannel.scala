package filterAccess.json

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Parse and build JSON objects contained by content channel packets.
 *
 */

/**
 *
 * Parses JSON objects contained by content channel packets.
 *
 */
object ContentChannelParser extends ChannelParser{

  implicit val formats = DefaultFormats

  /**
   * Extracts the contents name from a JSON object.
   * @param JSONObject JSON object containing data
   * @return Contents name
   */
  def getName(JSONObject: String): Option[String] = {

    val extractor = (m:JValue) => m.extract[Track].content
    getElement[String](JSONObject, extractor)

  }

  /**
   * Extracts the trace from a JSON object.
   * @param JSONObject
   * @return Trace as List of TrackPoints
   */
  def getTrace(JSONObject: String): Option[List[TrackPoint]] = {

    val extractor = (m:JValue) => m.extract[Track].trace
    getElement[List[TrackPoint]](JSONObject, extractor)

  }

}

/**
 *
 * Builds JSON objects contained by content channel packets.
 *
 */
object ContentChannelBuilder {

  implicit val formats = DefaultFormats

  def buildTrack(trace: List[TrackPoint], contentName: String): String = {

    val json =
      (
        ("content" -> contentName) ~
          ("trace" ->
            trace.map {
              point => ("x" -> point.x) ~ ("y" -> point.y) ~ ("z" -> point.z)
            }
            )
        )

    compact(render(json))
  }

}
