package filterAccess.json

import filterAccess.json.ContentChannelParser._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import scala.util.{Failure, Success, Try}


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Parse and build JSON objects contained by access channel packets.
 *
 */



/**
 *
 * Parses JSON objects contained by access channel packets.
 *
 */
object AccessChannelParser extends ChannelParser{

  implicit val formats = DefaultFormats

  /**
   * Extracts the access level of a certain user from a JSON object.
   * @param JSONObject JSON object with access permissions
   * @param node Node to extract access permission
   * @return Access level
   */
  def getAccessLevel(JSONObject: String, node: String): Option[Int] = {

    val extractor = (m:JValue) => {
      m.extract[Permissions].permissions
        .filter(userLevel => userLevel.name == node)
        .minBy(_.level)
        .level
    }

    getElement[Int](JSONObject, extractor)

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
