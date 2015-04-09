package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Makes permission data persistent
 *
 * For now, persistent data is hard-coded in this file.
 * In the long run, data might accessed through a database.
 */
object PermissionPersistency {

  /**
   *
   * @param name
   * @return
   */
  def getPersistentPermission(name: String): Option[String] = {

    name match {
      case "/stadtlauf2015" => Some(generatePermissionsJSON("/stadtlauf2015"))
      case "/paris-marathon" => Some(generatePermissionsJSON("/paris-marathon"))
      case "/jungfraujoch" => Some(generatePermissionsJSON("/jungfraujoch"))
      case _ => None

    }
  }

}
