package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Custom Exceptions for access control and filtering.
 *
 */
object Exceptions {


  /**
   * Throw this kind of exception if the service invocation should not return any answer.
   * Examples: Permission denied, Invalid access level..
   * @param d Description
   */
  case class noReturnException(d:String) extends Exception(d)

  /**
   * Throw this kind of exception if a service can not fulfil its task because fetching of
   * required data over the network failed.
   * @param d Description
   */
  case class dataUnavailableException(d:String) extends Exception(d)

}
