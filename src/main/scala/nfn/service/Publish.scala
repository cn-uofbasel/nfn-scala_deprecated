package nfn.service

import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, MetaInfo}
import nfn.NFNApi

class Publish() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    args match {
      case Seq(NFNContentObjectValue(contentName, contentData), NFNContentObjectValue(_, publishPrefixNameData), _) => {
        val nameOfContentWithoutPrefixToAdd = CCNName(new String(publishPrefixNameData).split("/").tail:_*)
        ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd, contentData, MetaInfo.empty), prependLocalPrefix = true)
        NFNEmptyValue()
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName can only be applied to arguments of type CCNNameValue and not: $args")
    }
  }
}
