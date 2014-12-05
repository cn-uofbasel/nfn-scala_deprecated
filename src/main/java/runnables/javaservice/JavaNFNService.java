package runnables.javaservice;

import akka.actor.ActorRef;
        import ccn.packet.CCNName;
        import nfn.service.CallableNFNService;
        import nfn.service.NFNService;
        import nfn.service.NFNService$class;
        import nfn.service.NFNValue;
        import scala.Option;
        import scala.collection.Seq;
        import scala.util.Try;

abstract class JavaNFNService implements NFNService {
    @Override
    public Option<Object> executionTimeEstimate() {
        return NFNService$class.executionTimeEstimate(this);
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
