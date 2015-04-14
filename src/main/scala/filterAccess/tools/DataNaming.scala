package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Handling of raw data names.
 *
 */
object DataNaming {

  /**
   * xxx
   * @param    n   xxx
   * @return       xxx
   */
  def unapply(n:String): Option[(String,String,String)] = {
    val parts = n split "//"
    if(parts.length == 3) Some(parts(0), "/" + parts(1), "/" + parts(2))
    else None
  }

  /**
   * Extract name of actual data.
   * @param    n   xxx
   * @return       xxx
   */
  def getName(n:String): Option[String] = {
    unapply(n) match {
      case Some((_, _, name)) => Some(name)
      case _ => None
    }
  }

  /**
   * Extract type of actual data.
   * @param    n   xxx
   * @return       xxx
   */
  def getType(n:String): Option[String] = {
    unapply(n) match {
      case Some((_, t, _)) => Some(t.substring(1))
      case _ => None
    }
  }

  // Concept: Extractors
  // Programming in Scala, Second Edition,
  // By Martin Odersky, LexSpoon, Bill Venners
  // Chapter 26: Extractors

}
