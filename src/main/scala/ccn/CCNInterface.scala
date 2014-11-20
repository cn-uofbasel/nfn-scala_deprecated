package ccn

import ccn.packet.{CCNPacket, Content, Interest}

import scala.concurrent.{ExecutionContext, Future}

trait CCNInterface {
  def wireFormat: CCNWireFormat
  def mkBinaryInterest(interest: Interest)(implicit ec: ExecutionContext): Future[Array[Byte]]
  def mkBinaryContent(content: Content)(implicit ec: ExecutionContext): Future[List[Array[Byte]]]
  def wireFormatDataToXmlPacket(binaryPacket: Array[Byte])(implicit ec: ExecutionContext): Future[CCNPacket]
  def mkAddToCacheInterest(content: Content)(implicit ec: ExecutionContext): Future[List[Array[Byte]]]
}
