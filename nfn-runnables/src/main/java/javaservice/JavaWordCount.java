package javaservice;

import akka.actor.ActorRef;
import ccn.packet.CCNName;
import nfn.service.CallableNFNService;
import nfn.service.NFNService;
import nfn.service.NFNService$class;
import nfn.service.NFNValue;
import scala.Function2;
import scala.Option;
import scala.collection.Seq;
import scala.util.Try;

/**
* Created by basil on 07/11/14.
*/
public class JavaWordCount implements NFNService {
    @Override
    public Option<Object> executionTimeEstimate() {
        return NFNService$class.executionTimeEstimate(this);
    }

    @Override
    public Function2<Seq<NFNValue>, ActorRef, NFNValue> function() {

        Function2<Seq<NFNValue>, ActorRef, NFNValue> f = new Function2<Seq<NFNValue>, ActorRef, NFNValue>();

        return null;
    }

    @Override
    public Try<CallableNFNService> instantiateCallable(CCNName name, Seq<NFNValue> values, ActorRef ccnServer, Option<Object> executionTimeEstimate) {
        return NFNService$class.instantiateCallable(this, name, values, ccnServer, executionTimeEstimate);
    }

    @Override
    public CCNName ccnName() {
        return NFNService$class.ccnName(this);
    }

    @Override
    public boolean pinned() {
        return false;
    }
}
