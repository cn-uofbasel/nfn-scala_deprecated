package nfn.service.Http

/**
 * Defines the traits implemented by any HTTP-Provider that can be used by the HttpServce (NFN Service).
 *
 * Author:  Ralph Gasser
 * Date:    27-02-2015
 * Version: 1.0
 */
trait HttpProvider {
  def head(urlString: String, header : Option[Map[String,String]] = None): String
  def get(urlString: String, header : Option[Map[String,String]] = None): String
  def delete(urlString: String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None): String
  def post(urlString: String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None): String
  def put(urlString: String, header : Option[Map[String,String]] = None, body: Option[Array[Byte]] = None): String
}
