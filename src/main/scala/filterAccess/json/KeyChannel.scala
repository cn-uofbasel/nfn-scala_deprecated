package filterAccess.json

import akka.actor.ActorRef
import filterAccess.json.AccessChannelParser._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import scala.collection.breakOut


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Parse and build JSON objects contained by key channel packets.
 *
 */

/**
 *
 * Parses JSON objects contained by key channel packets.
 *
 */
object KeyChannelParser extends ChannelParser {

  implicit val formats = DefaultFormats

  /**
   * Extracts the key to a certain access level from a JSON object.
   * @param JSONObject JSON object with keys
   * @param level Access level whose key should be returned
   * @return Key for the passed level
   */
  def extractLevelKey(JSONObject: String, level: Int): Option[String] = {

    // Extractor to get map[Int,Int] mapping AccessLevel to LevelKey
    val extractor = (m:JValue) => {

      // extract keys
      val list = (m \ "keys").children.children

      // convert to map
      val x:Map[Int,String] = (for(e <- list) yield ((e \ "level").extract[Int] -> (e \ "key").extract[String]))(breakOut)
      x

    }

    //extract key for given level
    extractElement[Map[Int,String]](JSONObject, extractor) match {
      case m:Some[Map[Int,String]] => m.get.get(level)
      case None => None
    }

  }

  /**
   * Check if certain user is allowed to access certain data at certain level
   *
   * @param   data     JSON object with permission data
   * @param   node     Identifier of the node
   * @param   level    Access level
   * @return           True, if access is allowed. False, if access is denied or json can not be parsed.
   */
  def checkPermission(data: String, node: String, level: Int): Boolean = {

    //parse access level from json
    val real_level = getAccessLevel(data, node)

    // checking permissions
    real_level match {
      case Some(l) => {
        // parsing successful, actual data extraction
        l <= level
      }
      case None => {
        // parsing failed
        false
      }
    }
  }

}

/**
 *
 * Builds JSON objects contained by key channel packets.
 *
 */
object KeyChannelBuilder {

  implicit val formats = DefaultFormats

  def buildKeys(keyList: Map[AccessLevel, LevelKey], contentName: String): String = {

    val json =
      (
        ("content" -> contentName) ~
          ("keys" ->
            keyList.map {
              k => ("level" -> k._1.level) ~ ("key" -> k._2.key.toString)
            }
            )
        )

    compact(render(json))
  }

}
