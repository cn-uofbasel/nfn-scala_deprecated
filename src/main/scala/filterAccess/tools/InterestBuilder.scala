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

  /**
   * Hard-coded ccn names of services for content, key and access/permission channel.
   *
   */
  val accessChannelName: CCNName = CCNName("node/node2/filterAccess_service_access_AccessChannel")
  val keyChannelName: CCNName = CCNName("node/node2/filterAccess_service_key_KeyChannel")
  val contentChannelStorageName: CCNName = CCNName("node/node1/filterAccess_service_content_ContentChannelStorage")
  val contentChannelProcessingName: CCNName = CCNName("node/node2/filterAccess_service_content_ContentChannelProcessing")

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
    accessChannelName call (name)

  /**
   * Build an Interest to fetch a key by name and access level and encrypted with a given public key.
   *
   * @param      name          Content Name
   * @param      accessLevel   Access Level
   * @param      id            Public Key
   * @return                   Interest
   */
  def buildKeyChannelInterest(name: String, accessLevel: Int, id: Int): Interest = {

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

    assert(accessLevel >= 0)

    accessLevel match {
      case al if al == 0 || al == 1 => contentChannelStorageName call(name, al)
      case al if al > 1 => contentChannelProcessingName call(name, 1)
    }

  }

}