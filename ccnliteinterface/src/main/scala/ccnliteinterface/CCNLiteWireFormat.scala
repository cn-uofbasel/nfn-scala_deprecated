package ccnliteinterface

object CCNLiteWireFormat {
  def fromName(possibleFormatName: String): Option[CCNLiteWireFormat] = {
    possibleFormatName match {
      case "ccnb" => Some(CCNBWireFormat())
      case "ndntlv" => Some(NDNTLVWireFormat())
      case "ccntlv" => Some(CCNTLVWireFormat())
      case _ => None
    }
  }
}

trait CCNLiteWireFormat
case class CCNBWireFormat() extends CCNLiteWireFormat {
  override def toString = "ccnb"
}
case class NDNTLVWireFormat() extends CCNLiteWireFormat {
  override def toString = "ndn2013"
}
case class CCNTLVWireFormat() extends CCNLiteWireFormat {
  override def toString = "ccnx2014"
}

