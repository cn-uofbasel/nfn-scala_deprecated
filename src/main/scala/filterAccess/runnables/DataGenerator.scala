package filterAccess.runnables

import filterAccess.dataGenerator.SimpleNDNExData._
import filterAccess.tools.Logging._
import java.io.{File, FileWriter}


/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Generates data for NDNExSetup by making use of the generator class [[filterAccess.dataGenerator.SimpleNDNExData]].
 * The generated data is stored in files under a certain path (see configuration section).
 *
 * ATTENTION: If a file already exists, its content is overwritten!
 *
 * - - - - - - - - - -
 *
 * Content Channel
 *  Track with 6 track points
 *
 * Key Channel
 *  AES-256 Keys for access level 1, 2, 3
 *
 * Permission Channel
 *  Certain Permissions for 3 different users
 *
 * - - - - - - - - - -
 *
 *     ../repo
 *     `-- ch
 *        `-- unibas
 *            `-- data
 *                `-- track
 *                    |-- jungfraujoch
 *                    |   |-- access_channel
 *                    |   |-- content_channel
 *                    |   `-- key_channel
 *                    |-- paris-marathon
 *                   |   |-- access_channel
 *                    |   |-- content_channel
 *                    |   `-- key_channel
 *                    `-- stadtlauf2015
 *                        |-- access_channel
 *                        |-- content_channel
 *                        `-- key_channel
 *
 *
 **/

object DataGenerator extends App {

  /**
   * Create a directory if it does not exist.
   * @param   path   Path of the directory
   */
  def createDir(path:String): Unit = {
    val d = new File(path)
    d.mkdirs()
    info("Created directory " + d.toString)
  }

  /**
   * Write to a file (overwrite in file already exists)
   * @param     path     Path of the file
   * @param     content  Content of the file
   */
  def writeToFile(path:String, content:String): Unit = {
    val f = new FileWriter(path)
    f.write(content)
    f.close()
    info(s"Wrote to file " + path)
  }


  // -----------------------------------------------------------------------------
  // ==== CONFIGURATION ==========================================================
  // -----------------------------------------------------------------------------

  section("configuration")

  /** Location of the data repository in the local file system */
  val storageLocation = "/home/claudio/mt/repo"
  /** Relative data name (rdn) prefix */
  val prefix = "/ch/unibas/data"
  /** List of available data types */
  val dataTypes = List("track")

  // - - - - -

  /** List of names of content objects of type track (with parameter i for data generator) */
  val tracks = List (
    ("stadtlauf2015", 9),
    ("paris-marathon", 12),
    ("jungfraujoch", 16)
  )

  // - - - - -

  info(s"Location of persistent data:     ${storageLocation}")
  info(s"Data prefix:                     ${prefix}")



  // -----------------------------------------------------------------------------
  // ==== DIRECTORY STRUCTURE ====================================================
  // -----------------------------------------------------------------------------

  section("create directory structure")

  // base directory
  subsection(s"Ensure that base directory exists")
  val baseDir = storageLocation + prefix
  createDir(baseDir)

  // directories for data types
  subsection(s"Ensure that directories for data types exist")
  dataTypes.foreach(name => createDir(baseDir + "/" + name))

  // directories for actual data
  subsection(s"Ensure that directories for actual data exist")
  dataTypes.foreach(track => {
    tracks.foreach(e => createDir(storageLocation + prefix + "/track/" + e._1))
  })



  // -----------------------------------------------------------------------------
  // ==== DIRECTORY STRUCTURE ====================================================
  // -----------------------------------------------------------------------------

  section("create files with data")

  subsection("Content Channel")

  // track
  tracks.foreach(e => {
    val path = storageLocation + prefix + "/track/" + e._1 + "/" + "content_channel"
    val content = generateTrackJSON("/" + e._1)
    writeToFile(path, content)
  })

  subsection("Key Channel")

  // track
  tracks.foreach( e => {
    val path = storageLocation + prefix + "/track/" + e._1 + "/" + "key_channel"
    val content = generateKeysJSON("/" + e._1, e._2)
    writeToFile(path, content)
  })

  subsection("Access Channel")

  // track
  tracks.foreach(e => {
    val path = storageLocation + prefix + "/track/" + e._1 + "/" + "access_channel"
    val content = generatePermissionsJSON("/" + e._1)
    writeToFile(path, content)
  })

}

