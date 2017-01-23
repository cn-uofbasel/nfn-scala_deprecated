package nfn.service

// ActorRef is the reference to an Akka Actor where you can send messages to
// It is used to have access to the client-library style interface to CCN where you can send interests to and
// receive content from (as well as access to the management interface and more)
// This service will no make use of this
import akka.actor.ActorRef
import ccn.packet.CCNName


// NFNService is a trait, which is very similar to a Java interface
// It requires the implementation of one method called 'function'
class Reverse extends NFNService{


  // This method does not have any parameters, you can imagine 'function()'.
  // The return type is a function, which has two parameters, one is a sequence (or list) of NFNValue's and the second
  // is the reference to the actor providing the CCN interface. This function returns a value of type NFNValue.
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    // Match the arguments to the expected or supported types
    // In this case the function is only implemented for a single parameter of type string ('foo bar')
    args match{
      case Seq(NFNStringValue(str)) =>

        // Return a result of type NFNValue, in this case a string value
        // NFNValue is a trait with a 'toDataRepresentation', which will be called on the result of the
        // function invocation to get the result to put into the final content object
        NFNStringValue(str.reverse)

      // ??? is a Scala construct, it throws a NotImplementedExeption (and is of type Nothing which is a subtype of any other type)
      case _ => ???
    }
  }
}
