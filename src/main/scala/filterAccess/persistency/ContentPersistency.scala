package filterAccess.persistency

import filterAccess.dataGenerator.SimpleNDNExData._
import filterAccess.tools.ConfigReader.getValueOrDefault

import scala.io.Source.fromFile

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
  val fromFilesystem = getValueOrDefault("dsu.fromFilesystem", "true") match {
    case Some(b)  => {
      b match {
        case "true" => true
        case "false" => false
        case _ => true
      }
    }
    case None => true
  }

  /**
   * Filesystem path of the data repository. This path is just used if fromFilesystem is set true.
   * Use [[filterAccess.runnables.DataGenerator]] to populate the data repository.
   *
   */
  val storageLocation = getValueOrDefault("dsu.repoPath", "/home/claudio/mt/repo").get

  /**
   * Get content channel data by name
   *
   * @param     name     Name of content
   * @return             Content as JSON object
   */
  def getPersistentContent(name:String): Option[String] = {

    // take from filesystem or generate on the fly?
    fromFilesystem match {
      case true => readDataFileSystem(name)
      case false => generateContent(name)
    }

  }

  /**
   * Read data from filesystem.
   *
   * @param     name     Name of content
   * @return             Content as JSON object
   */
  private def readDataFileSystem(name:String): Option[String] = {

    // path of file in the file system containing the asked data
    val path = storageLocation + name + "/content_channel"

    // read file
    // TODO - error handling
    val source = fromFile(path)
    val content = try source.getLines.mkString finally source.close

    // return
    Some(content)

  }

  /**
   * Generate content on the fly.
   *
   * @param     name     Name of content
   * @return             Content as JSON object
   */
  private def generateContent(name: String): Option[String] = {

        // generate data on the fly
        name match {
          case "/stadtlauf2015" => Some(generateTrackJSON("/stadtlauf2015"))
          case "/paris-marathon" => Some(generateTrackJSON("/paris-marathon"))
          case "/jungfraujoch" => Some(generateTrackJSON("/jungfraujoch"))
          case _ => None
        }

  }

}
