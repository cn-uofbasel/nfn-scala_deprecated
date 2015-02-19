package filterAccess.json

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import scala.util.{Failure, Success, Try}

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
object ContentChannelParser {

  implicit val formats = DefaultFormats

  /**
   * Extracts the contents name from a JSON object.
   * @param JSONObject JSON object containing data
   * @return Contents name
   */
  def getName(JSONObject: String): Option[String] = {

    // parsing
    val triedParsedJson: Try[JValue] = Try(parse(JSONObject))
    triedParsedJson match {
      case Success(parsedJson) => {
        try Some (parsedJson.extract[Track].content)
        catch {
          case _:Throwable => None // in most cases: user not found
        }
      }

      case Failure(e) => None // parsing failed

    }

  }

  /**
   * Extracts the trace from a JSON object.
   * @param JSONObject
   * @return Trace as List of TrackPoints
   */
  def getTrace(JSONObject: String): Option[List[TrackPoint]] = {

    // parsing
    val triedParsedJson: Try[JValue] = Try(parse(JSONObject))
    triedParsedJson match {
      case Success(parsedJson) => {
        try Some(parsedJson.extract[Track].trace)
        catch {
          case _:Throwable => None // in most cases: user not found
        }
      }

      case Failure(e) => None // parsing failed

    }

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
