package filterAccess.tools

import ccn.packet.{CCNName, Interest}

import lambdacalculus.parser.ast.LambdaDSL._
import nfn.LambdaNFNImplicits._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Helper functions for build an Interest for content/key/permission channel.
 *
 * Terminology:
 *  Direct    -  Access without proxy
 *  Indirect  -  Access with proxy
 *
 */
object InterestBuilder {

  //--------------------------------------------------------------------------------------------------------------


  /*
  *
  *   NOTE: LEAVE AWAY THE "/" AT THE BEGINNING! WHY?
  *
  *
  *   OBSERVATION:
  *   The following line of code...
  *     println(CCNName("/this/is/the/name").name)
  *   ..prints out:
  *     //this/is/the/name
  *
  *    WORK-AROUND:
  *      CCNName( "/this/is/the/name".substring(1) )
  *
  *
  *   THIS LOOKS LIKE A BUG IN NFN-SCALA.
  *
  */

  //--------------------------------------------------------------------------------------------------------------

  /*
   *
   * NOTE: The names of the services should principally be ContentChannel, KeyChannel, PermissionChannel.
   * Unfortunately, the current nfn-scala implementation does not support custom service names, but generates
   * its own names due to the package structure in scala.
   * To overcome this situation, we hard code the names generated names in this file.
   *
   * After introducing custom service names, this file should be simplified by implementing only three functions
   * to generate interests for key/content/permission channel. For now, we have to work with much more functions.
   *
   */

  //--------------------------------------------------------------------------------------------------------------


  /** Name of service for permission channel (direct) */
  val permissionChannelDirectName = "filterAccess_service_permission_PermissionChannelStorage"

  /** Name of service for permission channel (indirect) */
  val permissionChannelIndirectName= "filterAccess_service_permission_ProxyPermissionChannel"


  /** Name of service for key channel (direct) */
  val keyChannelDirectName = "filterAccess_service_key_KeyChannelStorage"

  /** Name of service for key channel (indirect) */
  val keyChannelIndirectName = "filterAccess_service_key_ProxyKeyChannel"


  /** Name of service for content channel (storage) (direct)*/
  val contentChannelStorageDirectName = "filterAccess_service_content_ContentChannelStorage"

  /** Name of service for content channel (storage) (indirect) */
  val contentChannelStorageIndirectName = "filterAccess_service_content_ProxyContentChannel"


  /** Name of service for content channel (filtering) (direct) */
  val contentChannelFilteringDirectName = "filterAccess_service_content_ContentChannelFiltering"

  /** Name of service for content channel (filtering) (indirect) */
  val contentChannelFilteringIndirectName = "filterAccess_service_content_ProxyContentChannel"


  /**
   * Build an Interest to fetch permission data by name.
   *
   * Direct access (without proxy)
   *
   * @param      rdn        Content Name
   * @param      prefix     Prefix of the service
   * @return                Interest
   */
  def buildDirectPermissionChannelInterest(rdn: String, prefix: String): Interest =
    CCNName(prefix.substring(1) + "/" + permissionChannelDirectName) call(rdn)


  /**
   * Build an Interest to fetch permission data by name.
   *
   * Indirect access (with proxy)
   *
   * @param      rdn           Content Name
   * @param      prefix        Prefix of the service
   * @return                   Interest
   */
  def buildIndirectPermissionChannelInterest(rdn:String, prefix: String): Interest =
    CCNName(prefix.substring(1) + "/" + permissionChannelIndirectName) call(rdn)


  /**
   * Build an Interest to fetch a key by name and access level and encrypted with a given public key.
   *
   * Direct access (without proxy)
   *
   * @param      rdn           Content Name
   * @param      prefix        Prefix of the service
   * @param      accessLevel   Access Level
   * @param      id            Public Key
   * @return                   Interest
   */
  def buildDirectKeyChannelInterest(rdn: String, prefix: String, accessLevel: Int, id: String): Interest = {
    assert(accessLevel >= 0)
    CCNName(prefix.substring(1) + "/" + keyChannelDirectName) call(rdn, accessLevel, id)
  }


  /**
   * Build an Interest to fetch a key by name and access level and encrypted with a given public key.
   *
   * Indirect access (with proxy)
   *
   * @param      rdn           Content Name
   * @param      prefix        Prefix of the service
   * @param      accessLevel   Access Level
   * @param      id            Public Key
   * @return                   Interest
   */
  def buildIndirectKeyChannelInterest(rdn:String, prefix: String, accessLevel: Int, id: String): Interest = {
    assert(accessLevel >= 0)
    CCNName(prefix.substring(1) + "/" + keyChannelIndirectName) call(rdn, accessLevel, id)
  }


  /**
   * Build an Interest to content by name and access level.
   *
   * Direct access (without proxy)
   *
   * Note that the caller of this function might have to adjust the prefix to the accessLevel (storage or filtering?)
   *
   * @param      rdn           Content Name
   * @param      prefix        Prefix of the service
   * @param      accessLevel   Access Level
   * @return                   Interest
   */
  def buildDirectContentChannelInterest(rdn: String, prefix: String, accessLevel: Int): Interest = {

    assert(accessLevel > 0)

    // distinct between storage and filtering
    accessLevel match {
      case 1 => CCNName(prefix.substring(1) + "/" + contentChannelStorageDirectName) call(rdn, 1)
      case l if l > 1 => CCNName(prefix.substring(1) + "/" + contentChannelFilteringDirectName) call(rdn, l)
    }

  }


  /**
   * Build an Interest to content by name and access level.
   *
   * Indirect access (with proxy)
   *
   * Note that the caller of this function might have to adjust the prefix to the accessLevel (storage or processing?)
   *
   * @param      rdn           Content Name
   * @param      prefix        Prefix of the service
   * @param      accessLevel   Access Level
   * @return                   Interest
   */
  def buildIndirectContentChannelInterest(rdn: String, prefix: String, accessLevel: Int): Interest = {

    assert(accessLevel > 0)

    // distinct between storage and filtering
    accessLevel match {
      case 1 => CCNName(prefix.substring(1) + "/" + contentChannelStorageIndirectName) call(rdn, 1)
      case l if l > 1 => CCNName(prefix.substring(1) + "/" + contentChannelFilteringIndirectName) call(rdn, l)
    }

  }

}
