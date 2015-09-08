package filterAccess.tools

import scala.sys.process._

import scala.language.postfixOps

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Retrieve values from config files.
 *
 * - - - - - - - - - - - - - - - - - - - - - - - -
 *
 * A config file contains key-value pairs such as:
 *  the.key = this is the value
 *  the.key=this is the value
 *    the.key    =    this is the value
 *
 * Comment out a key-value pair:
 *  # the.key = this key-value pair is commented out
 *
 * Annotate a key-value pair with a comment
 *  the.key = hello world # this is a comment...
 *
 * If a key is included more than once, the last entry is valid:
 *  the.key = this value is unimportant
 *  the.key = this value is valid
 *
 */
object ConfigReader {

  /** default path of config file */
  val defaultConfigPath = "~/nfn-scala.conf"

  /**
   * Try to extract a certain value specified by a certain key from a given config file location or default location.
   *
   * @param    key          Key
   * @param    configPath   Path to the config file (default: ~/nfn-scala.conf)
   * @return                Value if retrieved
   */
  def getValue(key: String, configPath:String = defaultConfigPath): Option[String] = {
    try {
      // run command to extract value from config file
      Seq( "/bin/sh", "-c",
          s"grep ^[[:space:]]*$key[[:space:]]*=[[:space:]]* $configPath |" +
          s"sed s/^.*=[[:space:]]*// |" +
          s"sed s/[[:space:]]*#.*// |" +
          "tail -1"
         ) !! match {
        // execution successful and result not empty
        case value: String if value.length > 0 => Some(value.trim)
        // execution successful but empty result (key not contained by config file?)
        case _ => None
      }

    }
    catch {
      // execution failed. config file not found?
      case _:Throwable => None
    }
  }

  /**
   * Try to extract a certain value specified by a certain key from a given config file location or default location.
   * If extraction failed, a passed default value is returned.
   *
   * @param    key            Key
   * @param    defaultValue   Default value
   * @param    configPath     Path to the config file (default: ~/nfn-scala.conf)
   * @return                  Retrieved value or otherwise default value
   */
  def getValueOrDefault(key:String, defaultValue:String, configPath:String=defaultConfigPath): Option[String] = {
    getValue(key, configPath) match {
      case None => Some(defaultValue.trim)
      case Some(v) => Some(v.trim)
    }
  }

}

