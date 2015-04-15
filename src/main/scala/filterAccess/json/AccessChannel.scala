package filterAccess.json

import net.liftweb.json._
import net.liftweb.json.JsonDSL._


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Parse and build JSON objects contained by access channel packets.
 *
 */


/**
 *
 * Parse JSON objects contained by access channel packets.
 *
 */
object AccessChannelParser extends ChannelParser{

  implicit val formats = DefaultFormats

  /**
   * Extract the access level of a certain user from a JSON object.
   *
   * @param     JSONObject    JSON object
   * @param     node          User
   * @return                  Access level
   */
  def getAccessLevel(JSONObject: String, node: String): Option[Int] = {

    val extractor = (m:JValue) => {
      m.extract[Permissions].permissions
        .filter(userLevel => userLevel.name == node)
        .minBy(_.level)
        .level
    }

    extractElement[Int](JSONObject, extractor)

  }

}


/**
 *
 * Build JSON objects contained by access channel packets.
 *
 */
object AccessChannelBuilder {

  implicit val formats = DefaultFormats

  /**
   * Build a JSON object contained by access channel packets.
   *
   * @param    userLevel      List of users with access level
   * @param    contentName    Raw data name
   * @return                  JSON object
   */
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
