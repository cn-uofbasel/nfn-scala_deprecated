package filterAccess.tools

import ccn.packet._

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
   *
   *   THIS LOOKS LIKE A BUG IN NFN-SCALA.
   *
   */

  /** Hard-coded ccn name of service for permission channel (direct) */
  val permissionChannelDirectName: CCNName = CCNName("serviceprovider/health/processing/filterAccess_service_permission_PermissionChannelStorage")

  /** Hard-coded ccn name of service for permission channel (indirect) */
  val permissionChannelIndirectName: CCNName = CCNName("own/machine/filterAccess_service_permission_ProxyPermissionChannel")


  /** Hard-coded ccn name of service for key channel (direct) */
  val keyChannelDirectName: CCNName = CCNName("serviceprovider/health/processing/filterAccess_service_key_KeyChannelStorage")

  /** Hard-coded ccn name of service for key channel (indirect) */
  val keyChannelIndirectName: CCNName = CCNName("own/machine/filterAccess_service_key_ProxyKeyChannel")


  /** Hard-coded ccn name of service for content channel (storage) (direct)*/
  val contentChannelStorageDirectName: CCNName = CCNName("serviceprovider/health/storage/filterAccess_service_content_ContentChannelStorage")

  /** Hard-coded ccn name of service for content channel (storage) (indirect) */
  val contentChannelStorageIndirectName: CCNName = CCNName("own/machine/filterAccess_service_content_ProxyContentChannel")


  /** Hard-coded ccn name of service for content channel (processing) (direct) */
  val contentChannelProcessingDirectName: CCNName = CCNName("serviceprovider/health/processing/filterAccess_service_content_ContentChannelProcessing")

  /** Hard-coded ccn name of service for content channel (processing) (indirect) */
  val contentChannelProcessingIndirectName: CCNName = CCNName("own/machine/filterAccess_service_content_ProxyContentChannel")


  /**
   * Build an Interest to fetch permission data by name.
   * 
   * Direct access (without proxy)
   *
   * @param      name          Content Name
   * @return                   Interest
   */
  def buildDirectPermissionChannelInterest(name: String): Interest =
    permissionChannelDirectName call(name)

  /**
   * Build an Interest to fetch permission data by name.
   *
   * Indirect access (with proxy)
   *
   * @param      name          Content Name
   * @return                   Interest
   */
  def buildIndirectPermissionChannelInterest(name:String): Interest =
    permissionChannelIndirectName call(name)

  /**
   * Build an Interest to fetch a key by name and access level and encrypted with a given public key.
   *
   * Direct access (without proxy)
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @param      id            Public Key
   * @return                   Interest
   */
  def buildDirectKeyChannelInterest(name: String, accessLevel: Int, id: String): Interest = {

    assert(accessLevel >= 0)
    keyChannelDirectName call(name, accessLevel, id)

  }

  /**
   * Build an Interest to fetch a key by name and access level and encrypted with a given public key.
   *
   * Indirect access (with proxy)
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @param      id            Public Key
   * @return                   Interest
   */
  def buildIndirectKeyChannelInterest(name:String, accessLevel: Int, id: String): Interest = {

    assert(accessLevel >= 0)
    keyChannelIndirectName call(name, accessLevel, id)

  }


  /**
   * Build an Interest to content by name and access level.
   *
   * Direct access (without proxy)
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @return                   Interest
   */
  def buildDirectContentChannelInterest(name: String, accessLevel: Int): Interest = {

    assert(accessLevel > 0)

    // distinct between storage and processing
    accessLevel match {
      case 1 => contentChannelStorageDirectName call(name, 1)
      case l if l > 1 => contentChannelProcessingDirectName call(name, l)
    }

  }

  /**
   * Build an Interest to content by name and access level.
   *
   * Indirect access (with proxy)
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @return                   Interest
   */
  def buildIndirectContentChannelInterest(name: String, accessLevel: Int): Interest = {

    assert(accessLevel > 0)

    // distinct between storage and processing
    accessLevel match {
      case 1 => contentChannelStorageIndirectName call(name, 1)
      case l if l > 1 => contentChannelProcessingIndirectName call(name, l)
    }

  }

}


