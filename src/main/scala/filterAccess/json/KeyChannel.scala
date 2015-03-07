package filterAccess.json

import filterAccess.json.AccessChannelParser._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

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

    // BUG in net.liftweb.json?
    // See extractElement(...) in ChannelParser.scala
    // See also: https://stackoverflow.com/questions/20157102/representing-a-list-of-json-tuples-as-a-case-class-field-with-json4s

    val extractor = (m:JValue) => {
      m.extract[Keys]
        .keys
        .find(e => e._1.level == level)
        .last // TODO exception if there exists no key for this level?
        ._2
        .key
    }

    val debug_extractor = (m:JValue) => {
      m.extract[Keys]
    }

    println("====> " + extractElement[Keys](JSONObject, debug_extractor))
    /// extractElement[Int](JSONObject, extractor)

    Some(99) // TODO - Solve issue mentioned above

  }

}

/**
 *
 * Builds JSON objects contained by key channel packets.
 *
 */
object KeyChannelBuilder {

  implicit val formats = DefaultFormats

  def buildKeys(keyList: List[(AccessLevel, LevelKey)], contentName: String): String = {

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
