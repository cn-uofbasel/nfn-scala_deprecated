package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Makes actual data persistent
 *
 * For now, persistent data is hard-coded in this file.
 * In the long run, data might be accessed through a database.
 */
object ContentPersistency {

  /**
   * xxx
   *
   * @param     name     Name of actual content
   * @return             xxx
   */
  def getPersistentContent(name: String): Option[String] = {

    name match {
      case "/stadtlauf2015" => Some(generateTrackJSON("/stadtlauf2015"))
      case "/paris-marathon" => Some(generateTrackJSON("/paris-marathon"))
      case "/jungfraujoch" => Some(generateTrackJSON("/jungfraujoch"))
      case _ => None

    }
  }

}
