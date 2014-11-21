package ccn.packet

import com.typesafe.scalalogging.slf4j.Logging

object CCNName {
  val thunkInterestKeyword = "THUNK"
  val thunkKeyword = "THUNK"
  val nfnKeyword = "NFN"
  val computeKeyword = "COMPUTE"
  def withAddedNFNComponent(ccnName: CCNName) = CCNName(ccnName.cmps ++ Seq(nfnKeyword) :_*)
  def withAddedNFNComponent(cmps: Seq[String]) = CCNName(cmps ++ Seq(nfnKeyword) :_*)

  def fromString(name: String):Option[CCNName] = {
    if(!name.startsWith("/")) None
    else {
      Some(CCNName(name.split("/").tail:_*))
    }
  }

  def apply(cmps: String *): CCNName = CCNName(cmps.toList, None)
}



case class CCNName(cmps: List[String], chunkNum: Option[Int])extends Logging {

  import CCNName.{thunkKeyword, nfnKeyword, computeKeyword}

//  def to = toString.replaceAll("/", "_").replaceAll("[^a-zA-Z0-9]", "-")
  override def toString = {
    cmps.toList.mkString("/", "/", "") + chunkNum.map({ cn => s"/c=$cn" }).getOrElse("")
  }

  private def isThunkWithKeyword(keyword: String) = cmps.size >= 2 && cmps.last == nfnKeyword && !cmps.drop(cmps.size-2).forall(_ != keyword)

  def isThunk: Boolean = isThunkWithKeyword(thunkKeyword)

  def isNFN: Boolean = cmps.size >= 1 && cmps.last == nfnKeyword

  def isCompute: Boolean = cmps.size >= 1 && cmps.head == computeKeyword

  def withoutCompute: CCNName = {
    if(cmps.size > 0) {
      if(cmps.head == computeKeyword) CCNName(cmps.tail:_*)
      else this
    } else this
  }

  def withoutNFN: CCNName = {
    if(cmps.size > 0) {
      if(cmps.last == nfnKeyword) CCNName(cmps.init:_*)
      else this
    } else this
  }

  def expression: Option[String] = {
    if(cmps.size > 0) {
      val expr = this.withoutCompute.withoutNFN
      expr.cmps match {
        case h :: Nil => Some(h)
        case _ =>
          logger.debug(s"name $this does not contain an expression")
          None
      }
    } else None
  }

  private def withoutThunkAndIsThunkWithKeyword(keyword: String): (CCNName, Boolean) = {
    if(cmps.size == 0) this -> false                                       // name '/' is not a thunk
    cmps.last match {
      case t if t == keyword =>                                                 // thunk of normal ccn name like /docRepo/doc/1/THUNK
        CCNName(cmps.init:_*) -> true                                      // return /docRepo/doc/1 -> true
      case t if t == nfnKeyword =>                                                   // nfn thunk like /add 1 1/(maybe: THUNK)/NFN
        val cmpsWithoutNFNCmp = cmps.init                                  // remove last nfn tag
        if(cmpsWithoutNFNCmp.size == 0) this -> false
        cmpsWithoutNFNCmp.last match {                                     // name '/NFN' is not a thunk
          case t2 if t2 == keyword =>                                             // nfn thunk like /add 1 1/THUNK/NFN
            CCNName(cmpsWithoutNFNCmp.init ++ Seq(nfnKeyword):_*) -> true  // return /add 1 1/NFN
          case _ => this -> false                                          //return /add 1 1/NFN -> false (original name)
        }
      case _ =>                                                            // normal ccn name like /docRepo/doc/1
        this -> false                                                      // return /docRepo/doc/1 -> false (original name)
    }
  }

  def withoutThunk: CCNName = withoutThunkAndIsThunk._1

  def withoutThunkAndIsThunk: (CCNName, Boolean) = withoutThunkAndIsThunkWithKeyword(thunkKeyword)

  private def thunkifyWithKeyword(keyword: String): CCNName = {
    cmps match {
      case _ if cmps.last == nfnKeyword =>
        cmps.init match {
          case cmpsWithoutLast if cmpsWithoutLast.last == keyword => this
          case cmpsWithoutLast => CCNName(cmpsWithoutLast ++ Seq(keyword, nfnKeyword): _*)
        }
      case _ if cmps.last == keyword => this
      case _ => CCNName(cmps ++ Seq(keyword): _*)
    }
  }

  def thunkify: CCNName = thunkifyWithKeyword(thunkKeyword)

  def append(cmpsToAppend:String*):CCNName = CCNName(cmps ++ cmpsToAppend:_*)
  def prepend(cmpsToPrepend:String*):CCNName = CCNName(cmpsToPrepend ++ cmps:_*)
  def append(nameToAppend:CCNName):CCNName = append(nameToAppend.cmps:_*)
  def prepend(nameToPrepend:CCNName):CCNName = prepend(nameToPrepend.cmps:_*)

  // Helper to improve java interop
  def cmpsList = cmps.toList
}

sealed trait CCNPacket {
  def name: CCNName
}

object NFNInterest {
  def apply(cmps: String *): Interest = Interest(CCNName(cmps ++ Seq("NFN") :_*))
}

object Interest {
  def apply(cmps: String *): Interest = Interest(CCNName(cmps :_*))
}
case class Interest(name: CCNName) extends CCNPacket {

  def this(cmps: String *) = this(CCNName(cmps:_*))

  def thunkify: Interest = Interest(name.thunkify)
}


object MetaInfo {
  val empty = MetaInfo(None)
}
case class MetaInfo(chunkNum: Option[Int])

object Content {
  def apply(data: Array[Byte], cmps: String *): Content =
    Content(CCNName(cmps :_*), data, MetaInfo.empty)

  def thunkForName(name: CCNName, executionTimeEstimated: Option[Int]) = {
    val thunkyfiedName = name.thunkify
    val thunkContentData = executionTimeEstimated.fold("")(_.toString)
    Content(thunkyfiedName, thunkContentData.getBytes, MetaInfo.empty)
  }
  def thunkForInterest(interest: Interest, executionTimeEstimate: Option[Int]): Content = {
    thunkForName(interest.name, executionTimeEstimate)
  }
}

case class Content(name: CCNName, data: Array[Byte], metaInfo: MetaInfo = MetaInfo.empty) extends CCNPacket {

  def possiblyShortenedDataString: String = {
    val dataString = new String(data)
    val dataStringLen = dataString.length
    if(dataStringLen > 50)
      dataString.take(50) + "..." + dataString.takeRight(10)
    else dataString
  }
  override def toString = s"Content[$metaInfo]('$name' => '$possiblyShortenedDataString' [size=${data.size}])"
}

case class Nack(name: CCNName) extends CCNPacket {
  val content: String = ":NACK"

  def toContent = Content(name, content.getBytes, MetaInfo.empty)
}

case class AddToCacheAck(name: CCNName) extends CCNPacket
case class AddToCacheNack(name: CCNName) extends CCNPacket

