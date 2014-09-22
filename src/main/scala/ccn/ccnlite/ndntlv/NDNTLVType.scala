package ccn.ccnlite.ndntlv

import ccn.ccnlite.ndntlv.tlvtranscoding._


object NDNTLVType {
  class NDNTLVException(msg: String) extends Exception(msg)
}
trait NDNTLVType {
  def typ: Int

  def encodeData: Array[Byte] = {
    TLVEncoder.encode(encodeTLV).toArray
  }

  def encodeTLV:TLV = {
    val data = tlvData
    TLV(typ, data.length, data)
  }


  def tlvData: List[Byte]

}

trait RecursiveType extends NDNTLVType {

  def unapply: List[Option[NDNTLVType]]

  override def tlvData: List[Byte] = {
    TLVEncoder.encode(
      unapply map { _ map { _.encodeTLV }}
    )
  }

}

trait EmptyType extends NDNTLVType {
  override def tlvData: List[Byte] = Nil

}

trait DataType extends NDNTLVType {
  override def tlvData: List[Byte] = data

  def data: List[Byte]

}

trait StringType extends NDNTLVType {
  override def tlvData: List[Byte] = str.getBytes.toList

  def str: String
}

trait NonNegativeIntegerType extends NDNTLVType {
  def tlvData = TLVEncoder.longToNonNegativeInteger(nonNegInt)

  def nonNegInt: Long
}

