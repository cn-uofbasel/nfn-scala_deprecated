package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Helper functions for logging and output.
 *
 */
object Logging {

  /** print info? */
  val print_info = true

  /** print warning? */
  val print_warning = true

  /** print section? */
  val print_section = true

  /** print subsection? */
  val print_subsection = true

  /** print hline? */
  val print_hline = true

  /** prefix of output */
  val prefix = " |>> "


  /*** NOTE: AKKA output is configurable in src/main/resources/reference.conf ***/


  /**
   * Print info message
   *
   * @param   m   Message
   */
  def info(m:String): Unit = {
    if (print_info == true)
      println(prefix + m)
  }


  /**
   * Print warning
   *
   * @param   m   Message
   */
  def warning(m:String): Unit = {
    if (print_warning == true)
      println(prefix + m)
  }


  /**
   * Print section
   *
   * @param   t   Title
   */
  def section(t:String): Unit = {
    if (print_warning == true)
      println("\n======= " + t.toUpperCase + " =======")
  }


  /**
   * Print subsection
   *
   * @param   t   Title
   */
  def subsection(t:String): Unit = {
    if (print_warning == true)
      println("\n--- " + t.toUpperCase + " ---")
  }


  /**
   * Print horizontal line
   */
  def hline: Unit = {
    if (print_hline == true)
      println(" | - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ")

  }

}
