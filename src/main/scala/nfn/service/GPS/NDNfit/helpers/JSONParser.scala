package nfn.service.GPS.NDNfit.helpers

import net.liftweb.json._

import scala.util.{Failure, Success, Try}


object JSONParser {

  /**
    * Extract certain information from a JSON object.
    *
    * Error handling is implemented by this function while the actual extraction
    * logic is passed as a functional literal (extractor). To gain flexibility,
    * the type of the return value is determined by the caller.
    *
    * @param    JSONObject    JSON Object containing the asked data
    * @param    extractor     Function which does the actual extraction
    * @tparam   ReType        Type of returned object
    * @return                 Extracted data
    */
  def extractElement[ReType](JSONObject: String, extractor: (JValue => ReType)): Option[ReType] = {

    val ast: Try[JValue] = Try(parse(JSONObject))
    ast match {
      case Success(parsedJson) => {
        // actual data extraction
        try {
          Some(extractor(parsedJson))
        }
        catch {
          // JSONObject is valid JSON, but extraction still failed.
          case e:Throwable => {
            // For Debugging: Comment in the following line..
            // e.printStackTrace
            None
          }
        }
      }

      // Invalid JSON
      case Failure(e) => None

    }

  }

  def getLong(JSONObject: String): Option[String] = {
    ???
    // use extractElement
  }

  def getLat(JSONObject: String): Option[String] = {
    ???
    // use extractElement
  }

  def getTime(JSONObject: String): Option[String] = {
    ???
    // use extractElement
  }

}
