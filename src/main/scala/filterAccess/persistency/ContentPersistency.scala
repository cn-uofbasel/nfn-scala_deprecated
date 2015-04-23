package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Persistency of content channel data.
 *
 * For now, persistent data is generated on the fly or taken from the filesystem.
 *
 */
object ContentPersistency {

  /**
   * If true, data is taken from the filesystem.
   * Otherwise it us generated on the fly.
   */
  val fromFilesystem = false

  /**
   * Filesystem path of the data repository. This path is just used if fromFilesystem is set true.
   * Use [[filterAccess.runnables.DataGenerator]] to populate the data repository.
   *
   */
  val storageLocation = "/home/claudio/mt/repo"

  /**
   * Get content channel data by name
   *
   * @param     name     Name of content
   * @return             Content as JSON object
   */
  def getPersistentContent(name: String): Option[String] = {

    // take from filesystem or generate on the fly?
    fromFilesystem match {
      case true => {
        // take data from file system
        ???
      }
      case false => {
        // generate data on the fly
        name match {
          case "/stadtlauf2015" => Some(generateTrackJSON("/stadtlauf2015"))
          case "/paris-marathon" => Some(generateTrackJSON("/paris-marathon"))
          case "/jungfraujoch" => Some(generateTrackJSON("/jungfraujoch"))
          case _ => None
        }
      }
    }

  }

}
