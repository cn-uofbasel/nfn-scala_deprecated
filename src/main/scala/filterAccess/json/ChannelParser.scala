package filterAccess.json

import net.liftweb.json._

import scala.util.{Failure, Success, Try}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
trait ChannelParser {

  def getElement[ReType](JSONObject: String, extractor: (JValue => ReType)): Option[ReType] = {

    val triedParsedJson: Try[JValue] = Try(parse(JSONObject))
    triedParsedJson match {
      case Success(parsedJson) => {
        // actual data extraction
        try {
          Some(extractor(parsedJson))
        }
        catch {
          case _: Throwable => None // in most cases: user not found
        }
      }

      case Failure(e) => None

    }

  }

}




