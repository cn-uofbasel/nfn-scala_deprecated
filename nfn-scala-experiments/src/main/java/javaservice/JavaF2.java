//package javaservice;
//
//import akka.actor.ActorRef;
//import ccn.packet.CCNName;
//import nfn.service.*;
//import scala.*;
//import scala.collection.Seq;
//import scala.util.Try;
//
//public class JavaF2 implements Function2<Seq<NFNValue>, ActorRef, NFNValue> {
//
//    NFNValue res = new NFNStringValue("res");
//
//    @Override
//    public NFNValue apply(Seq<NFNValue> v1, ActorRef v2) {
//        return new NFNStringValue("res");
//    }
//
//    @Override
//    public Function1<Seq<NFNValue>, Function1<ActorRef, NFNValue>> curried() {
//        return null;
//    }
//
//    @Override
//    public Function1<Tuple2<Seq<NFNValue>, ActorRef>, NFNValue> tupled() {
//        return null;
//    }
//}
//
//
