package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Persistency of permission channel data.
 *
 * For now, persistent data is hard-coded in this file.
 * In the long run, data might be accessed through a database.
 */
object PermissionPersistency {

  /**
   * Get permission channel data by name
   *
   * @param     name     Name of content
   * @return             Permissions as JSON object
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
