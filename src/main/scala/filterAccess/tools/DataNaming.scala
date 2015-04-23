package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Handling of relative data names.
 *
 */
object DataNaming {

  /**
   * Extract components of a relative data name.
   *
   * @param    n   Relative data name
   * @return       Name components of n (prefix, type, name)
   */
  def unapply(n:String): Option[(String,String,String)] = {
    val parts = n split "//"
    if(parts.length == 3) Some(parts(0), "/" + parts(1), "/" + parts(2))
    else None
  }

  /**
   * Extract name component of a relative data.
   *
   * @param    n   Relative data name
   * @return       Name component of n
   */
  def getName(n:String): Option[String] = {
    unapply(n) match {
      case Some((_, _, name)) => Some(name)
      case _ => None
    }
  }

  /**
   * Extract type component of a relative data name.
   *
   * @param    n   Relative data name
   * @return       Type component of n
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
