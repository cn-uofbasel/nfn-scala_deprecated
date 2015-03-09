package filterAccess.json

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
  def extractLevelKey(JSONObject: String, level: Int): Option[Int] = {

    // Extractor to get map[Int,Int] mapping AccessLevel to LevelKey
    val extractor = (m:JValue) => {

      // extract keys
      val list = (m \ "keys").children.children

      // convert to map
      val x:Map[Int,Int] = (for(e <- list) yield ((e \ "level").extract[Int] -> (e \ "key").extract[Int]))(breakOut)
      x

    }

    //extract key for given level
    extractElement[Map[Int,Int]](JSONObject, extractor) match {
      case m:Some[Map[Int,Int]] => m.get.get(level)
      case None => None
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
              k => ("level" -> k._1.level) ~ ("key" -> k._2.key)
            }
            )
        )

    compact(render(json))
  }

}
