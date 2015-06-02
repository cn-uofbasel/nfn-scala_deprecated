package nfn.service.Http

import akka.actor.ActorRef
import nfn.service._

/**
 * NFN Service that can be used to access HTTP and HTTPS resources. The following HTTP methods are supported: HEAD, GET, PUT, POST, DELETE
 *
 * Usage:
 *
 * call http {method} {url} [--body {body}] [--header {header field name 1} {header field value 1} ...]
 *
 * Author:  Ralph Gasser
 * Date:    27-02-2015
 * Version: 1.0
 */
class HttpService() extends NFNService {
  /** Instance of the class that provides the basic HTTP functionality. Must implement the HttpProviderTrait. */
  val provider : HttpProvider = new SimpleHttpProvider();

  /** Main function for the HTTP Service. Matches arguments and issues the HTTP/S request using the HttpProvider class.
   *
   * @param args
   * @param ccnApi
   * @return NFNValue
   */
  override def function(args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match{
      case Seq (NFNStringValue(method), url : NFNValue) => executeRequest(method, new String(url.toDataRepresentation))
      case Seq (NFNStringValue(method), url : NFNValue, NFNStringValue("--header"), header @ _*) => executeRequest(method, new String(url.toDataRepresentation), extractHeader(header))
      case Seq (NFNStringValue(method), url : NFNValue, NFNStringValue("--body"), body : NFNValue) => executeRequest(method, new String(url.toDataRepresentation), None, Some(body.toDataRepresentation))
      case Seq (NFNStringValue(method), url : NFNValue, NFNStringValue("--body"), body : NFNValue, NFNStringValue("--header"), header @ _*) => executeRequest(method, new String(url.toDataRepresentation), extractHeader(header), Some(body.toDataRepresentation))
      case _ => throw new NFNServiceArgumentException("The provided signature is not supported. Use <method> <url> [--body <body>] [--header <header>] instead.")
    }
  }


  /** Maps the method argument to the respective function in the HttpProvider class. The other arguments are just passed through.
   *
   * @param method String specifying the HTTP method.
   * @param url URL that should be invoked.
   * @param header Map[String, String] that contains header fields and values.
   * @param body Array of bytes containing the HTTP body.
   * @return NFNStringValue
   */
  private [HttpService] def executeRequest (method : String, url : String, header : Option[Map[String, String]] = None, body : Option[Array[Byte]] = None): NFNStringValue = {
    val result = method.toUpperCase() match {
      case "GET" => provider.get(url, header)
      case "HEAD" => provider.head(url, header)
      case "POST" => provider.post(url, header, body)
      case "PUT" => provider.put(url, header, body)
      case "DELETE" => provider.delete(url, header, body)
      case _ => throw new NFNServiceArgumentException("The provided HTTP method is not supported. Use GET, HEAD, POST, PUT or DELETE instead.")
    }
    result match {
      case result : String => NFNStringValue(result)
      case _ => throw new NFNServiceExecutionException("The HTTP request did not return a response.")
    }
  }

  /** Maps a sequence of NFNStringValues to a Map by using neighbouring elements as key/value pairs.
   *
   * @param args Seq[NFNValue] The sequence of NFNValues.
   * @return Map[String, String]
   */
  private [HttpService] def extractHeader (args: Seq[NFNValue]): Option[Map[String,String]] = Some(args.grouped(2) map {case Seq(NFNStringValue(a),NFNStringValue(b)) => (a,b)} toMap)
}
