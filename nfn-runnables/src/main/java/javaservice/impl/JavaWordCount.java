//package javaservice.impl;
//
//import akka.actor.ActorRef;
//import ccn.packet.CCNName;
//import nfn.service.*;
//import scala.*;
//import scala.collection.Seq;
//import scala.runtime.AbstractFunction2;
//import scala.util.Try;
//
//
//
//public class JavaWordCount implements NFNService{
//    @Override
//    public Option<Object> executionTimeEstimate() {
//        // rewiring of trait implementations
//        return NFNService$class.executionTimeEstimate(this);
//    }
//
//    @Override
//    public Function2<Seq<NFNValue>, ActorRef, NFNValue> function() {
//        // AbstractFunction2 rewires all trait implementations from Function2$class to the abstract class
//        // only actual trait "interface" needs to be implemented
//        return new AbstractFunction2<Seq<NFNValue>, ActorRef, NFNValue>() {
//            @Override
//            public NFNValue apply(Seq<NFNValue> v1, ActorRef v2) {
//                return new NFNIntValue(42);
//            }
//        };
//    }
//
//    @Override
//    public Try<CallableNFNService> instantiateCallable(CCNName name, Seq<NFNValue> values, ActorRef ccnServer, Option<Object> executionTimeEstimate) {
//        // rewiring of trait implementations
//        return NFNService$class.instantiateCallable(this, name, values, ccnServer, executionTimeEstimate);
//    }
//
//    @Override
//    public CCNName ccnName() {
//        // rewiring of trait implementations
//        return NFNService$class.ccnName(this);
//    }
//
//    @Override
//    public boolean pinned() {
//        return false;
//    }
//}
