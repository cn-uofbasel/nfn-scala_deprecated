package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._

import scala.io.Source._

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Persistency of key channel data (symmetric keys).
 *
 * For now, persistent data is generated on the fly or taken from the filesystem.
 *
 */
object KeyPersistency {

  /**
   * If true, data is taken from the filesystem.
   * Otherwise it us generated on the fly.
   */
  val fromFilesystem = true

  /**
   * Filesystem path of the data repository. This path is just used if fromFilesystem is set true.
   * Use [[filterAccess.runnables.DataGenerator]] to populate the data repository.
   *
   */
  val storageLocation = "/home/claudio/mt/repo"


  /**
   * Get key channel data by name
   *
   * @param     name     Name of content
   * @return             Key data as JSON object
   */
  def getPersistentKey(name:String): Option[String] = {

    // take from filesystem or generate on the fly?
    fromFilesystem match {
      case true => readKeyFileSystem(name)
      case false => generateKey(name)
    }

  }

  /**
   * Read data from filesystem.
   *
   * @param     name     Name of content
   * @return             Content as JSON object
   */
  private def readKeyFileSystem(name:String): Option[String] = {

    // path of file in the file system containing the asked data
    // TODO - data prefix is hard coded for now. Generalize this..
    val path = storageLocation + "/ch/unibas/data/track" + name + "/key_channel"

    // read file
    // TODO - error handling
    val source = fromFile(path)
    val content = try source.getLines.mkString finally source.close

    // return
    Some(content)

  }

  /**
   * Generate key data on the fly.
   *
   * @param     name     Name of content
   * @return             Key data as JSON object
   */
  private def generateKey(name: String): Option[String] = {

    // take from filesystem or generate on the fly?
    fromFilesystem match {
      case true => {
        // take data from file system
        ???
      }
      case false => {
        // generate data on the fly
        name match {
          case "/stadtlauf2015" => Some(generateKeysJSON("/stadtlauf2015", 9))
          case "/paris-marathon" => Some(generateKeysJSON("/paris-marathon", 12))
          case "/jungfraujoch" => Some(generateKeysJSON("/jungfraujoch", 16))
          case _ => None
        }
      }
    }

  }

}
