package filterAccess.tools

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Handling of relative data names.
 *
 * THIS IS PARTIALLY OUTDATED DUE TO CHANGED STRUCTURE OR RELATIVE DATA NAMES!
 *
 */
object DataNaming {

  /**
   * Extract components of a relative data name.
   *
   * @param    n   Relative data name
   * @return       Name components of n (prefix, type, name)
   */
  def unapply(n:String): Option[(String,String)] = {
    val parts = n split "@"
    if(parts.length == 2) Some(parts(0), parts(1))
    else None
  }

  /**
   * Extract rdn component of a extended relative data name.
   * Extended relative data name is of following form: <rdn>@<prefix>
   *
   * @param    n   Extended relative data name
   * @return       RDN component of n
   */
  def getRDN(n:String): Option[String] = {
    unapply(n) match {
      case Some((name, _)) => Some(name)
      case _ => None
    }
  }

  /**
   * Extract prefix component of a extended relative data name.
   * Extended relative data name is of following form: <rdn>@<prefix>
   *
   * @param    n   Extended relative data name
   * @return       Prefix component of n
   */
  def getPrefix(n:String): Option[String] = {
    unapply(n) match {
      case Some((_, prefix)) => Some(prefix)
      case _ => None
    }
  }

  /**
   * Extract type component of a relative data name.
   *
   * THIS IS OUTDATED! DO NOT USE!
   *
   * @param    n   Relative data name
   * @return       Type component of n
   */
  def getType(n:String): Option[String] = {
    // for prototyping all data are of type track...
    Some("type:track") // TODO
  }

  // Concept: Extractors
  // Programming in Scala, Second Edition,
  // By Martin Odersky, LexSpoon, Bill Venners
  // Chapter 26: Extractors

}
