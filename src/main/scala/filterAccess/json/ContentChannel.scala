package filterAccess.json

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Parse and build JSON objects contained by content channel packets.
 *
 */


/**
 *
 * Parse JSON objects contained by content channel packets.
 *
 */
object ContentChannelParser extends ChannelParser{

  implicit val formats = DefaultFormats

  /**
   * Extract raw data name from a JSON object.
   *
   * @param    JSONObject    JSON object
   * @return                 Raw data name
   */
  def getName(JSONObject: String): Option[String] = {

    val extractor = (m:JValue) => m.extract[Track].content
    extractElement[String](JSONObject, extractor)

  }

  /**
   * Extracts the trace from a JSON object with type "track".
   *
   * @param     JSONObject   JSON object
   * @return                 Trace as List of TrackPoints
   */
  def getTrace(JSONObject: String): Option[List[TrackPoint]] = {

    val extractor = (m:JValue) => m.extract[Track].trace
    extractElement[List[TrackPoint]](JSONObject, extractor)

  }

}

/**
 *
 * Build JSON objects contained by content channel packets.
 *
 */
object ContentChannelBuilder {

  implicit val formats = DefaultFormats

  /**
   * Build a JSON object of type "track" contained by content channel packets.
   *
   * @param    trace          List of track points
   * @param    contentName    Raw data name
   * @return                  JSON object
   */
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
