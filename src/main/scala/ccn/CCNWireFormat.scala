package ccn

object CCNWireFormat {
  def fromName(possibleFormatName: String): Option[CCNWireFormat] = {
    possibleFormatName match {
      case "ccnb" => Some(CCNBWireFormat())
      case "ndntlv" => Some(NDNTLVWireFormat())
      case "ccntlv" => Some(CCNTLVWireFormat())
      case _ => None
    }
  }
}

trait CCNWireFormat
case class CCNBWireFormat() extends CCNWireFormat {
  override def toString = "ccnb"
}
case class NDNTLVWireFormat() extends CCNWireFormat {
  override def toString = "ndn2013"
}
case class CCNTLVWireFormat() extends CCNWireFormat {
  override def toString = "ccnx2014"
}

