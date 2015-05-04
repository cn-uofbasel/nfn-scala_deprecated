package filterAccess.tools

import ccn.packet._

import lambdacalculus.parser.ast.LambdaDSL._
import nfn.LambdaNFNImplicits._


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Helper functions for build an Interest for content/key/permission channel.
 *
 */
object InterestBuilder {

  /** Hard-coded ccn name of service for permission channel */
  val permissionChannelName: CCNName = CCNName("serviceprovider/health/processing/filterAccess_service_permission_PermissionChannelStorage")

  /** Hard-coded ccn name of service for key channel */
  val keyChannelName: CCNName = CCNName("serviceprovider/health/processing/filterAccess_service_key_KeyChannelStorage")

  /** Hard-coded ccn name of service for content channel (storage) */
  val contentChannelStorageName: CCNName = CCNName("serviceprovider/health/storage/filterAccess_service_content_ContentChannelStorage")

  /** Hard-coded ccn name of service for content channel (processing) */
  val contentChannelProcessingName: CCNName = CCNName("serviceprovider/health/processing/filterAccess_service_content_ContentChannelProcessing")

  /*

   TODO

   NOTE: LEAVE AWAY THE "/" AT THE BEGINNING! WHY?

   OBSERVATION:
   The following line of code...
     println(CCNName("/this/is/the/name").name)
   ... prints out:
     //this/is/the/name

  */

  /**
   * Build an Interest to fetch permission data by name.
   *
   * @param      name          Content Name
   * @return                   Interest
   */
  def buildPermissionChannelInterest(name: String): Interest =
    permissionChannelName call (name)

  /**
   * Build an Interest to fetch a key by name and access level and encrypted with a given public key.
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @param      id            Public Key
   * @return                   Interest
   */
  def buildKeyChannelInterest(name: String, accessLevel: Int, id: String): Interest = {

    assert(accessLevel >= 0)
    keyChannelName call(name, accessLevel, id)

  }


  /**
   * Build an Interest to content by name and access level.
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @return                   Interest
   */
  def buildContentChannelInterest(name: String, accessLevel: Int): Interest = {

    assert(accessLevel > 0)

    // distinct between storage and processing
    accessLevel match {
      case 1 => contentChannelStorageName call(name, 1)
      case l if l > 1 => contentChannelProcessingName call(name, l)
    }

  }

}


