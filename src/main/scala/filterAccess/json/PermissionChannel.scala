package filterAccess.json

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import scala.collection.mutable


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Parse and build JSON objects contained by permission channel packets.
 *
 */


/**
 *
 * Parse JSON objects contained by permission channel packets.
 *
 */
object PermissionChannelParser extends ChannelParser{

  implicit val formats = DefaultFormats

  /**
   * Extract the access level of a certain user from a JSON object.
   *
   * @param     JSONObject    JSON object
   * @param     node          User
   * @return                  Access level
   */
  def getAccessLevel(JSONObject: String, node: String): Option[Int] = {

    val extractor = (m:JValue) => {
      m.extract[Permissions].permissions
        .filter(userLevel => userLevel.name == node)
        .minBy(_.level)
        .level
    }

    extractElement[Int](JSONObject, extractor)

  }


  /**
   * Extracts the content name from a JSON object containing permission data.
   *
   * @param   JSONObject   JSON object containing permission data
   * @return               Content name
   */
  def getName(JSONObject: String): Option[String] = {
    val extractor = (m:JValue) => m.extract[Permissions].content
    extractElement[String](JSONObject, extractor)
  }

  /**
   * Extracts the content name from a JSON object containing permission data.

   * @param   JSONObject   JSON object containing permission data
   * @return               List of publicKey/level pairs ([[UserLevel]]])
   */
  def getPermissions(JSONObject: String): Option[List[UserLevel]] = {
    val extractor = (m:JValue) => m.extract[Permissions].permissions
    extractElement[List[UserLevel]](JSONObject, extractor)
  }

}


/**
 *
 * Build JSON objects contained by permission channel packets.
 *
 */
object PermissionChannelBuilder {

  implicit val formats = DefaultFormats

  /**
   * Build a JSON object contained by permission channel packets.
   *
   * @param    userLevel      List of users with access level
   * @param    contentName    Relative data name
   * @return                  JSON object
   */
  def buildPermissions(userLevel: List[UserLevel], contentName: String): String = {

    val json =
      (
        ("content" -> contentName) ~
          ("permissions" ->
            userLevel.map {
              ul => ("name" -> ul.name) ~ ("level" -> ul.level)
            }
            )
        )

    compact(render(json))
  }


  /**
   * Takes a JSON object with permission data and restructures its permission entries due to a given function.
   * This is a general purpose function. the actual restructuring logic is passed as a functional literal.
   * Specialized functions should call this implementation.
   *
   * @param    fullJSON   Permissions to remove certain entries
   * @param    name       Name of the new JSON object
   * @param    restruct   Function to restructure the List[UserLevel]
   * @return              Restructured JSON object
   */
  def restructurePermissions(fullJSON:String, name:String, restruct:(List[UserLevel] => List[UserLevel])):String = {

    // restructure
    // TODO -- better error handling
    val restructuredUserLevel = restruct(PermissionChannelParser.getPermissions(fullJSON).get)

    // rebuild
    buildPermissions(restructuredUserLevel, name)

  }

  /**
   * Takes a JSON object with permission data, selects certain permission entries (dependant on level) and manipulates
   * the level. This function takes the functional literal "selector" to select the permission entries and "manipulator"
   * to manipulate them. Thereof, we gain generality. Specialized functions should call this implementation.
   * Note: This function might change the ordering of the list.
   *
   * @param    fullJSON      Permissions to remove certain entries
   * @param    name          Name of the new JSON object
   * @param    selector      Functional literal to select permission entries
   * @param    manipulator   Functional literal to manipulate selected entries
   * @return                 Manipulated JSON object
   */
  def manipulateLevel(fullJSON: String, name:String, selector:(UserLevel => Boolean), manipulator:(UserLevel => UserLevel)):String = {

    // select and manipulate
    val manipulatedEntries:List[UserLevel] = PermissionChannelParser.getPermissions(fullJSON).get.withFilter(selector).map(manipulator)
    val unchangedEntries:List[UserLevel] = PermissionChannelParser.getPermissions(fullJSON).get.filterNot(selector)

    // concat and rebuild json
    buildPermissions(unchangedEntries ::: manipulatedEntries, name)

  }


  /**
   * Takes a JSON object with permission data and removes all entries with an access level
   * less privileged (higher number) than a given level.
   *
   * @param    fullJSON   Permissions to remove certain entries
   * @param    level      Threshold
   * @return              Restricted Permissions as JSON object
   */
  def minimizePermissions(fullJSON:String, level:Int):String = {

    // extract and reduce
    val contentName = PermissionChannelParser.getName(fullJSON).get
    restructurePermissions(fullJSON, contentName, (l => l.filter(userLevel => (userLevel.level <= level))))

  }


  /**
   * Merges UserList of two JSON objects with permission data. Only entries are taken, which are contained
   * by both lists. If the access level differs, the level of the merged list is restricted to the one with
   * lower permissions.
   *
   * @param   name         Name of the new JSON object
   * @param   fullJSON1    JSON object with permissions
   * @param   fullJSON2    JSON object with permissions
   * @return               Merged Permissions as JSON Object
   */
  def conjunctiveMerge(name: String, fullJSON1:String, fullJSON2:String):String = {

    // extract permission data from JSON objects
    val list1:List[UserLevel] = PermissionChannelParser.getPermissions(fullJSON1).get
    val list2:List[UserLevel] = PermissionChannelParser.getPermissions(fullJSON2).get

    // find IDs contained by both JSON objects..
    var mergedList:List[UserLevel] = List[UserLevel]()
    for(l1 <- list1) {
      for(l2 <- list2) {
        if(l1.name == l2.name)
        // ..and take the higher access level
          mergedList = UserLevel(l1.name, math.max(l1.level,l2.level)) :: mergedList
      }
    }

    // rebuild
    buildPermissions(mergedList, name)

  }

}
