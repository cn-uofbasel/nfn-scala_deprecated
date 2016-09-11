package runnables.javaservice;

import akka.actor.ActorRef;
import ccn.packet.CCNName;
import nfn.service.*;
import scala.collection.Seq;
import scala.collection.JavaConverters;

/**
* Created by basil on 07/11/14.
*/
public class JavaWordCount extends JavaNFNService {
    @Override
    public NFNValue function(CCNName interestName, Seq<NFNValue> args, ActorRef nfnApi) {

        int numberOfWords = 0;
        for(NFNValue arg : JavaConverters.asJavaIterableConverter(args).asJava()) {
          numberOfWords += (new String(arg.toDataRepresentation())).split(" ").length;
        }
        return new NFNIntValue(numberOfWords);
    }
}


