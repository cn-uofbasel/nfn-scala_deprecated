package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Makes symmetric keys persistent.
 *
 * For now, persistent data is hard-coded in this file.
 * In the long run, data might be accessed through a database.
 */
object KeyPersistency {

  /**
   * xxx
   *
   * In the long run, this function should connect to a database, ohmage or ..
   *
   * @param     name     Name of actual content
   * @return             xxx
   */
  def getPersistentKey(name: String): Option[String] = {
    name match {
      case "/stadtlauf2015" => Some(generateKeysJSON("/stadtlauf2015", 9))
      case "/paris-marathon" => Some(generateKeysJSON("/paris-marathon", 12))
      case "/jungfraujoch" => Some(generateKeysJSON("/jungfraujoch", 16))
      case _ => None
    }

  }

}
