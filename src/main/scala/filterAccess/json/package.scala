package filterAccess

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Classes to which JSON maps
 *
 */
package object json {

  // Access Channel
  case class UserLevel(name: String, level: Int)
  case class Permissions(content: String, permissions: List[UserLevel])

  // Key Channel
  // TODO


  // Permission Channel
  // TODO

}
