package nfn

import ccn.packet.{CCNName, Content}
import com.typesafe.scalalogging.slf4j.Logging

import scala.collection.mutable

case class ChunkStore(size: Int, name: List[String]) extends Logging {
  val cs = Array.fill(size)(Option.empty[Array[Byte]])

  def add(chunkNum: Int, chunkData: Array[Byte]): Unit = {
    if(cs(chunkNum).isEmpty) {
      logger.debug(s"new chunk with chunkNum: $chunkNum / ${size - 1}")
      cs(chunkNum) = Some(chunkData)
    } else {
      logger.warn(s"Received chunk is already in chunk store, ignoring chunk with num $chunkNum")
    }
  }

  def isComplete: Boolean =  cs.forall(_.nonEmpty)
  def getComplete: Option[Content] = {
    if(cs.forall(_.nonEmpty)) {
      val data = cs.foldRight(Array[Byte]()) { case (head, tail) => head.get ++ tail }
      Some(Content(CCNName(name, None), data))
    } else None
  }

  def getIncomplete: List[Int] = {
    cs.zipWithIndex
      .filter({case (a, i) => a.isEmpty})
      .map({case (c, i) => i}).toList
  }
}

case class ContentStore() extends Logging {
  private val contentStore: mutable.Map[List[String], Content] = mutable.Map()
  private val chunkStore: mutable.Map[List[String], ChunkStore] = mutable.Map()

  def apply(name: CCNName): Option[Content] = get(name)
  def get(name: CCNName): Option[Content] = {
    name.chunkNum match {
      case Some(chunkNum) => chunkStore.get(name.cmpsList) flatMap { _.getComplete }
      case None => contentStore.get(name.cmpsList)
    }
  }

  def add(content: Content): Unit = {
    val name = content.name.cmpsList

    (content.name.chunkNum, content.metaInfo.chunkNum) match {
      case (Some(chunkNum), Some(lastChunkNum)) =>
        chunkStore.get(name) match {
          case Some(cs) => cs.add(chunkNum, content.data)
          case _ => {
            logger.debug(s"created chunkstore for name ${content.name}")
            val cs = ChunkStore(lastChunkNum + 1, name)
            cs.add(chunkNum, content.data)
            chunkStore += (name -> cs)
          }
        }
      case _ => {
        if(content.name.chunkNum.isDefined || content.metaInfo.chunkNum.isDefined)
          logger.warn(s"Found content $content with only one of either chunknum or lastchunknum, treating it as non-chunk content")
        contentStore += (name -> content)
      }
    }

  }
  def remove(name: CCNName): Unit = {
    name.chunkNum match {
      case Some(_) => logger.warn("remove for a single chunk not implemented")
      case None => contentStore -= name.cmps
    }
  }
  def getContentCompleteOrIncompletedChunks(name: CCNName): Either[Content, List[Int]] = {
    contentStore.get(name.cmps) match {
      case Some(content) => Left(content)
      case None =>
        chunkStore.get(name.cmps).map({ cs =>
          cs.getComplete match {
            case Some(completedContent) => {
              chunkStore -= name.cmps
              contentStore += (name.cmps -> completedContent)
              Left(completedContent)
            }
            case None => Right(cs.getIncomplete)
          }
        }).getOrElse(Right(Nil))
    }
  }

}

