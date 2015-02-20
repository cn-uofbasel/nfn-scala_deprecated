package filterAccess.json

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
  //TODO
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
