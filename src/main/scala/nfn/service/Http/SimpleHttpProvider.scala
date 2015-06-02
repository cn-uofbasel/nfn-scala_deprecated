package nfn.service.Http

import java.io._
import java.net.{HttpURLConnection, URL}
import javax.net.ssl.HttpsURLConnection

import nfn.service.NFNServiceArgumentException

/**
 * This class provides a convenient HTTP interface based on java.lang.HttpURLConnection. It supports the following
 * HTTP/1.1 methods: HEAD, GET, PUT, POST, DELETE
 *
 * Author:  Ralph Gasser
 * Date:    27-02-2015
 * Version: 1.0
 */
class SimpleHttpProvider extends HttpProvider {
  /** Shortcut methods defined by the HttpProviderTrait. */
  def head(urlString: String, header : Option[Map[String,String]] = None): String = executeRequest("HEAD", urlString, header)
  def get(urlString: String, header : Option[Map[String,String]] = None): String = executeRequest("GET", urlString, header)
  def delete(urlString: String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None): String = executeRequest("DELETE", urlString, header, body)
  def post(urlString: String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None): String = executeRequest("POST", urlString, header, body)
  def put(urlString: String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None): String = executeRequest("PUT", urlString, header, body)

  /** (Private) Main method of the SimpleHttpProvider class. Prepares the HTTP request using the (Java) HttpUrlConnection class, sends it and reads the HTTP response.
   *
   * @param method Http method that should be used. At the moment GET, HEAD, POST, PUT and DELETE are supported.
   * @param urlString String representation of the requested URL.
   * @param header Map of key-value pairs that should be used as HTTP header fields.
   * @param body Byte array cthat should be sent as HTTP body.
   * @return String representation of the HTTP response.
   */
  private[SimpleHttpProvider] def executeRequest(method : String, urlString : String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None) : String = {
    /* Create URL from provided base URL string and parameters. */
    /* Try to open a new URL connection. */
    var url: URL = new URL(urlString);
    val connection: HttpURLConnection = url.openConnection() match {
      case connection: HttpsURLConnection => connection
      case connection: HttpURLConnection => connection
      case _ => throw new ClassCastException
    }

    /* 1) Setup HTTP request: Specify HTTP method. */
    connection.setRequestMethod(method)

    /* 2) Setup HTTP request: Apply header fields to the HTTP request. */
    header.getOrElse(Map()).foreach {
      case(key,value) => {
        connection.setRequestProperty(key, value)
      }
    }

    /* 3) Setup HTTP request: Setup method specific parts of the request. */
    method match {
      case "GET" | "HEAD" => {
        connection.setDoOutput(false)
      }
      case "POST" | "PUT" | "DELETE" => {
        connection.setDoOutput(true)
        body match {
          case Some(x) => connection.setRequestProperty("Content-Length", x.size.toString);
          case None =>
        }
      }
      case _ => throw new NFNServiceArgumentException("The provided HTTP method '" + method + "' is not supported.")
    }

    /* 4) Setup HTTP request: Send request and wait for response. */
    this.sendRequest(connection, body) match {
      case Some(reader) => this.readResponse(reader)
      case None => null
    }
  }

  /** (Private) Method used to read the HTTP resonse
   *
   * @param reader BufferedReader to read the response from
   */
  private[SimpleHttpProvider] def readResponse(reader: BufferedReader): String = {
    try {
      val response = Stream.continually(reader.readLine()).takeWhile(_ != null).mkString("\n")
      reader.close()
      response
    } catch {
      case ex: IOException =>
        println("[ERROR] IO Exception while reading HttpResponse (" + ex.getMessage + ").")
        null
    }
  }

  /**
   * (Private) Method that, given a HttpURLConnection object, takes care of sending a HTTP request and handling IO exceptions during
   * sending. If sending of the request was successful, the method return a BufferedReader which can be used to read the response.
   *
   * @param connection Instance of HttpURLConnection used to send the request.
   * @return Option[BufferedReader] that can be used to read the HTTP response
   */
  private[SimpleHttpProvider] def sendRequest(connection: HttpURLConnection, body: Option[Array[Byte]] = None): Option[BufferedReader] = {
    /* Try to send request. */
    try {
      println("[INFO] Sending HTTP Request: " + connection.getRequestMethod + " " + connection.getURL.toString + ", Header-size: " + connection.getRequestProperties.size + " items, Body-size: " + body.getOrElse(Array()).size + " bytes");

      /* Write and send request .*/
      if (connection.getDoOutput == true) {
        val outputStream : DataOutputStream = new DataOutputStream(connection.getOutputStream)
        body match {
          case Some(x) => outputStream.write(x)
          case None =>
        }
        outputStream.flush()
        outputStream.close()
      }

      /* Return input stream reader if sending was successful: Read response */
      Some(new BufferedReader(new InputStreamReader(connection.getInputStream)))
    } catch {
      case ex: IOException =>
        println("[ERROR] IO Exception while sending HttpRequest (" + ex.getMessage + ").")
        connection.getErrorStream match {
          case stream: InputStream => Some(new BufferedReader(new InputStreamReader(stream)))
          case _ => None
        }
    }
  }
}
