package filterAccess.service.processing.track.distance

import akka.actor.ActorRef
import nfn.service._

import filterAccess.crypto.Helpers.{symKeyGenerator,computeHash}

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * General channel of the "distance" processing service.
 *
 */
abstract class Distance extends NFNService {

  /** public key (identity) */
  final protected var publicKey:String = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ=="

  /** corresponding private key */
  final protected var privateKey:String = "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="

  /**
   * Get publicKey (public key) of this service.
   *
   * @return  Public Key
   */
  final def getPublicKey: String = publicKey

  /**
   * Get private key corresponding to public key (publicKey).
   *
   * @return  Private Key
   */
  final def getPrivateKey: String = privateKey


  /**
   * Deterministically generates a symmetric encryption key out of another key and some additional parameters.
   *
   * @param    key      Given encryption key
   * @param    salt     Makes the synthesized key dependent from some additional information
   * @param    pepper   Same purpose as "salt"
   * @return            Synthesized key
   */
  final def keySyntesizer(key:String, salt:String, pepper:String): String =
    symKeyGenerator(computeHash(pepper + key + salt)) // TODO --- secure??


  /** Pin this service */
  override def pinned: Boolean = false

}