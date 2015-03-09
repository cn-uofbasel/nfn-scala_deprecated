package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Custom Exceptions
 *
 */
object Exceptions {


  /**
   * Throw this kind of exception when the service invocation should not return any answer.
   * Examples: Permission denied, Invalid access level..
   * @param m Message
   */
  case class noReturnException(m:String) extends Exception(m)

}
