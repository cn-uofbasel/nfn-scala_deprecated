package filter_access.json

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import scala.util.{Failure, Success, Try}


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Parse and build JSON objects contained by access channel packets.
 *
 */

// classes to which JSON maps
case class UserLevel(name: String, level: Int)

case class Permissions(content: String, permissions: List[UserLevel])

/**
 *
 * Parses JSON objects contained by access channel packets.
 *
 */
object AccessChannelParser {

  implicit val formats = DefaultFormats

  /**
   * Extracts the access level of a certain user from a JSON object.
   * @param JSONObject JSON object with access permissions
   * @param node Node to extract access permission
   * @return Access level
   */
  def getAccessLevel(JSONObject: String, node: String): Option[Int] = {

    // actual parsing
    val triedParsedJson: Try[JValue] = Try(parse(JSONObject))
    triedParsedJson match {
      case Success(parsedJson) => {
        // parsing successful
        Some {
          // actual data extraction
          parsedJson.extract[Permissions].permissions
            .filter(userLevel => userLevel.name == node)
            .minBy(_.level)
            .level
        }
      }

      // parsing failed
      case Failure(e) => {
        None
      }

    }

  }

}


/**
 *
 * Build JSON objects contained by access channel packets.
 *
 */
object AccessChannelBuilder {

  implicit val formats = DefaultFormats


  def buildPermissions(userLevel: List[UserLevel], contentName: String): String = {

    val json =
      (
        ("content" -> contentName) ~
          ("permissions" ->
            userLevel.map {
              ul => ("name" -> ul.name) ~ ("level" -> ul.level)
            }
            )
        )

    compact(render(json))
  }


}
