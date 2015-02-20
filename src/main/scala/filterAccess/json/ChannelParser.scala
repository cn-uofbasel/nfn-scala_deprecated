package filterAccess.json

import net.liftweb.json._

import scala.util.{Failure, Success, Try}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */


trait ChannelParser {

  /**
   * Extract certain information from a JSON object. Error handling is implemented by this function while
   * the actual extraction logic is passed as a functional literal (extractor). To gain flexibility, the
   * type of the return value is determined by the caller.
   * @param JSONObject JSON Object containing the asked data
   * @param extractor Function which does the actual extraction
   * @tparam ReType Type of returned Object.
   * @return Extracted data.
   */
  def extractElement[ReType](JSONObject: String, extractor: (JValue => ReType)): Option[ReType] = {

    val triedParsedJson: Try[JValue] = Try(parse(JSONObject))
    triedParsedJson match {
      case Success(parsedJson) => {
        // actual data extraction
        try {
          Some(extractor(parsedJson))
        }
        catch {
          // JSONObject is valid JSON, but extraction still failed.
          // Example: Access of a key that does not exist.
          case _: Throwable => None
        }
      }

      // Invalid JSON
      case Failure(e) => None

    }

  }

}




