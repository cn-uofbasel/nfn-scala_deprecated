package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 * Helper functions for logging.
 *
 */
object Logging {

  /**
   * Settings
   */
  val prefix = " |>> "
  val print_info = true
  val print_warning = true
  val print_section = true
  val print_subsection = true
  val print_hline = true

  // Note: Settings for akka output under src/main/resources/reference.conf

  /**
   * Print info message
   * @param m Message
   */
  def info(m:String): Unit = {
    if (print_info == true)
      println(prefix + m)
  }

  /**
   * Print warning
   * @param m Message
   */
  def warning(m:String): Unit = {
    if (print_warning == true)
      println(prefix + m)
  }

  /**
   * Print section
   * @param t Title
   */
  def section(t:String): Unit = {
    if (print_warning == true)
      println("\n======= " + t.toUpperCase + " =======")
  }

  /**
   * Print subsection
   * @param t Title
   */
  def subsection(t:String): Unit = {
    if (print_warning == true)
      println("\n--- " + t.toUpperCase + " ---")
  }

  def hline: Unit = {
    if (print_hline == true)
      println(" | - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ")

  }

}
