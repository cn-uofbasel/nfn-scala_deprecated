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

    println("----> "+ JSONObject +" <----")

    val ast: Try[JValue] = Try(parse(JSONObject))
    ast match {
      case Success(parsedJson) => {
        // actual data extraction
        try {
          Some(extractor(parsedJson))
        }
        catch {
          // JSONObject is valid JSON, but extraction still failed.
          case e:Throwable => e.printStackTrace; None
          // here might be a bug in net.liftweb.json
          // see: https://stackoverflow.com/questions/15281779/scala-type-parameter-seems-to-be-getting-stuck
          // see: https://github.com/lift/framework/issues/1417
        }
      }

      // Invalid JSON
      case Failure(e) => None

    }

  }

}




