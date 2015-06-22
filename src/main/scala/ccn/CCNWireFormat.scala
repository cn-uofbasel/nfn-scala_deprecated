package ccn

object CCNWireFormat {
  def fromName(possibleFormatName: String): Option[CCNWireFormat] = {
    possibleFormatName match {
      case "ccnb" => Some(CCNBWireFormat())
      case "ndntlv" => Some(NDNTLVWireFormat())
      case "ndn2013" => Some(NDNTLVWireFormat())
      case "ccntlv" => Some(CCNTLVWireFormat())
      case "ccnx2015" => Some(CCNTLVWireFormat())
      case "cistlv" => Some(CISTLVWireFormat())
      case "cisco2015" => Some(CISTLVWireFormat())
      case "iottlv" => Some(IOTTLVWireFormat())
      case "iot2014" => Some(IOTTLVWireFormat())
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
  override def toString = "ccnx2015"
}
case class CISTLVWireFormat() extends CCNWireFormat {
  override def toString = "cisco2015"
}
case class IOTTLVWireFormat() extends CCNWireFormat {
  override def toString = "iot2014"
}

