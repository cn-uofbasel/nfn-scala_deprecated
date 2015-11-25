package nfn.service

import java.nio.ByteBuffer

import ccn.packet.CCNName


trait NFNValue {
  def toCCNName: CCNName

  def toDataRepresentation: Array[Byte]
}

case class NFNContentObjectValue(name: CCNName, data: Array[Byte]) extends NFNValue {

  override def toCCNName: CCNName = name

  override def toDataRepresentation: Array[Byte] = data
}

case class NFNServiceValue(serv: NFNService) extends NFNValue {
  override def toDataRepresentation: Array[Byte] = serv.ccnName.toString.getBytes

  override def toCCNName: CCNName = serv.ccnName
}



case class NFNListValue(values: List[NFNValue]) extends NFNValue {

  override def toDataRepresentation: Array[Byte] = values.map({ _.toCCNName.toString }).mkString(" ").getBytes

  override def toCCNName: CCNName = CCNName(values.map({ _.toCCNName.toString }).mkString(" "))
}

case class NFNNameValue(name: CCNName) extends NFNValue{
  override def toDataRepresentation: Array[Byte] = name.toString.getBytes

  override def toCCNName: CCNName = name
}

case class NFNIntValue(i: Int) extends NFNValue {
  def apply = i

  override def toCCNName: CCNName = CCNName("Int")

  override def toDataRepresentation: Array[Byte] = i.toString.getBytes
}

case class NFNFloatValue(f: Double) extends NFNValue {
  def apply = f

  override def toCCNName: CCNName = CCNName("Float")

  override def toDataRepresentation: Array[Byte] = f.toString.getBytes
}

case class NFNStringValue(str: String) extends NFNValue {
  def apply = str

  override def toCCNName: CCNName = CCNName("String")

  override def toDataRepresentation: Array[Byte] = str.getBytes
}

case class NFNDataValue(data: Array[Byte]) extends NFNValue {
  def apply = data

  override def toCCNName: CCNName = CCNName("Array[Byte]")

  override def toDataRepresentation: Array[Byte] = data
}

case class NFNEmptyValue() extends NFNValue {

  override def toCCNName: CCNName = CCNName("EmptyValue")

  override def toDataRepresentation: Array[Byte] = Array[Byte]()
}
