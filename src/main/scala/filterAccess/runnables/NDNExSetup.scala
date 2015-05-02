package filterAccess.runnables


import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}

import filterAccess.service.key.{ProxyKeyChannel, KeyChannelStorage, KeyChannel}
import filterAccess.service.access.{ProxyAccessChannel, AccessChannelStorage, AccessChannel}
import filterAccess.service.content.ContentChannelStorage
import filterAccess.service.content.ContentChannelProcessing
import filterAccess.service.content.ProxyContentChannel

import monitor.Monitor
import node.LocalNodeFactory

import filterAccess.tools.Logging._

import filterAccess.crypto.Decryption._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import lambdacalculus.parser.ast.LambdaDSL._
import nfn.LambdaNFNImplicits._


object NDNExSetup extends App {

  implicit val conf: Config = ConfigFactory.load()
  implicit val useThunks: Boolean = false


  /*
   * Created by Claudio Marxer <marxer@claudio.li>
   *
   * SETUP:
   *  Network with dsu, dpu and dvu and proxy.
   *  Service "ContentChannelProcessing" on dpu.
   *  Services "KeyChannel" and "AccessChannel" and "ContentChannelStorage" on dsu.
   *  Service "ContentChannelProxy" on proxy.
   *


  {Content Channel Storage}
               {KeyChannel}
            {AccessChannel}     {ContentChannel Processing}
                      |                  |
                  +-------+          +-------+
                  |  dsu  |**********|  dpu  |
                  +-------+          +-------+
                        *              *
                         *            *
                          *          *
                           +-------+
                           | proxy | -- {Content Channel Proxy}
                           +-------+
                               *
                               *
                           +-------+
                           |  dvu  | <--- Sends out Interests...
                           +-------+        (CCN-only node)


   *
   *
   **/


  // -----------------------------------------------------------------------------
  // ==== NETWORK SETUP ==========================================================
  // -----------------------------------------------------------------------------

  // network setup
  val dsu = LocalNodeFactory.forId(1, prefix=Option(CCNName("serviceprovider", "health", "storage")))
  val dpu = LocalNodeFactory.forId(2, prefix=Option(CCNName("serviceprovider", "health", "processing")))
  val dvu = LocalNodeFactory.forId(3, prefix=Option(CCNName("personal", "device")), isCCNOnly = true)
  val proxy = LocalNodeFactory.forId(4, prefix=Option(CCNName("own", "machine")))
  val nodes = List(dsu, dpu, dvu, proxy)
  dsu <~> dpu
  dpu <~> proxy
  dsu <~> proxy
  dvu <~> proxy

  // routing
  dvu.registerPrefixToNodes(proxy, List(dsu, dpu)) // TODO --- check if this is set up the right way..

  section("network setup")

  println(
    """
         *  Network with dsu, dpu and dvu and proxy.
         *  Service "ContentChannelProcessing" on dpu.
         *  Services "KeyChannel" and "AccessChannel" and "ContentChannelStorage" on dsu.
         *  Service "ContentChannelProxy" on proxy.

        {Content Channel Storage}
                    {Key Channel}
                 {Access Channel}     {Content Channel Processing}
                            |                  |
                        +-------+          +-------+
                        |  dsu  |**********|  dpu  |
                        +-------+          +-------+
                              *              *
                               *            *
                                *          *
                                 +-------+
                                 | proxy | -- {Content Channel Proxy}
                                 +-------+
                                     *
                                     *
                                 +-------+
                                 |  dvu  | <--- Sends out Interests...
                                 +-------+        (CCN-only node)


    """)



  // -----------------------------------------------------------------------------
  // ==== SERVICE SETUP ==========================================================
  // -----------------------------------------------------------------------------

  section("service setup")

  // service setup (access/permission channel)
  subsection("access/permission channel")
  val accessChannelServ = new AccessChannelStorage
  accessChannelServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  accessChannelServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dpu.publishServiceLocalPrefix(accessChannelServ)
  val accessChannelName = dpu.localPrefix.append(accessChannelServ.ccnName)
  info(s"Access channel installed on dpu: $accessChannelName")
  info("Identity (public key) of access channel service: " + accessChannelServ.getPublicKey)

  // service setup (key channel - storage)
  subsection("key channel")
  val keyChannelServ = new KeyChannelStorage
  keyChannelServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  keyChannelServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dpu.publishServiceLocalPrefix(keyChannelServ)
  val keyChannelName = dpu.localPrefix.append(keyChannelServ.ccnName)
  info(s"Key channel installed on dpu: $keyChannelName")
  info("Identity (public key) of key channel service: " + keyChannelServ.getPublicKey)

  // service setup (content channel - storage)
  subsection("content channel (storage)")
  val contentChannelStorageServ = new ContentChannelStorage
  // contentChannelStorageServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  // contentChannelStorageServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dsu.publishServiceLocalPrefix(contentChannelStorageServ)
  val contentChannelStorageName = dsu.localPrefix.append(contentChannelStorageServ.ccnName)
  info(s"Content channel (storage) installed on dsu: $contentChannelStorageName")
  info("Identity (public key) of content channel service: " + contentChannelStorageServ.getPublicKey)

  // service setup (content channel - processing)
  subsection("content channel (processing)")
  val contentChannelProcessingServ = new ContentChannelProcessing
  // contentChannelProcessingServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  // contentChannelProcessingServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dpu.publishServiceLocalPrefix(contentChannelProcessingServ)
  val contentChannelProcessingName = dpu.localPrefix.append(contentChannelProcessingServ.ccnName)
  info(s"Content channel proxy installed on dpu: $contentChannelProcessingName")
  info("Identity (public key) of content channel service: " + contentChannelProcessingServ.getPublicKey)

  // -----------------------------------------------------------------------------
  // ==== PROXY SETUP ============================================================
  // -----------------------------------------------------------------------------

  section("proxy setup")


  // service setup (content channel)
  subsection("content channel")
  val proxyContentChannelServ = new ProxyContentChannel
  proxy.publishServiceLocalPrefix(proxyContentChannelServ)
  val proxyContentChannelName = proxy.localPrefix.append(proxyContentChannelServ.ccnName)
  info(s"Content channel proxy installed on proxy: $proxyContentChannelName")

  // service setup (access channel)
  subsection("access channel")
  val proxyAccessChannelServ = new ProxyAccessChannel
  proxy.publishServiceLocalPrefix(proxyAccessChannelServ)
  val proxyAccessChannelName = proxy.localPrefix.append(proxyAccessChannelServ.ccnName)
  info(s"Content channel proxy installed on proxy: $proxyAccessChannelName")

  // service setup (key channel)
  subsection("key channel")
  val proxyKeyChannelServ = new ProxyKeyChannel
  proxy.publishServiceLocalPrefix(proxyKeyChannelServ)
  val proxyKeyChannelName = proxy.localPrefix.append(proxyKeyChannelServ.ccnName)
  info(s"Content channel proxy installed on proxy: $proxyKeyChannelName")


  //============================================================================//
  //============================================================================//
  //============================================================================//


  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSIONS FROM DSU =============================================
  // -----------------------------------------------------------------------------

  if (false) {

    section("FETCH PERMISSIONS FROM DSU")

    Thread.sleep(1000)

    val interest_permissions: Interest = accessChannelName call ("/node/node2//type:track//stadtlauf2015")

    // send interest for permissions from dpu...
    val startTime2 = System.currentTimeMillis
    info("Send interest: " + interest_permissions)
    dvu ? interest_permissions onComplete {
      // ... and receive content
      case Success(resultContent) => {
        info("Result:        " + new String(resultContent.data))
        // import filterAccess.encoding.Decryption._
        // info(symDecrypt(new String(resultContent.data),165))
        info("Time:          " + (System.currentTimeMillis - startTime2) + "ms")
        Monitor.monitor ! Monitor.Visualize()
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
      }
    }
  }



  // -----------------------------------------------------------------------------
  // ==== FETCH KEY FROM DSU =====================================================
  // -----------------------------------------------------------------------------


  if (false) {
    section("FETCH KEY FROM DSU")

    Thread.sleep(1000)

    val interest_key: Interest = keyChannelName call("/node/node2//type:track//paris-marathon", 2, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dpu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    dvu ? interest_key onComplete {
      // ... and receive content
      case Success(resultContent) => {
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime3) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }
    }
  }


  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSION AS WELL AS KEY FROM DSU AND PERFORM ENCRYPTION ========
  // -----------------------------------------------------------------------------

  var permissionData: String = "to be fetched..."
  var keyData: String = "to be fetched..."

  if (false) {

    section("FETCH PERMISSION AS WELL AS KEY FROM DSU AND PERFORM ENCRYPTION")

    Thread.sleep(1000)
    subsection("Access Channel")

    val interest_permissions: Interest = accessChannelName call ("/node/node2//type:track//stadtlauf2015")

    // send interest for permissions from dpu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_permissions)
    dvu ? interest_permissions onComplete {
      // ... and receive content
      case Success(resultContent) => {
        permissionData = new String(resultContent.data)
        info("Result (Enc):  " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime4) + "ms")
        Monitor.monitor ! Monitor.Visualize()
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel")

    val interest_key: Interest = keyChannelName call("/node/node2//type:track//stadtlauf2015", 0, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dpu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    dvu ? interest_key onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result (Key):  " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime3) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }
    }

    Thread.sleep(1000)
    subsection("Encryption")
    info("Decrypted:     " + symDecrypt(permissionData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))

  }


  // -----------------------------------------------------------------------------
  // ==== FETCH AN UNPROCESSED TRACK WITH CONTENT CHANNEL FROM DSU AND DECRYPT ===
  // -----------------------------------------------------------------------------

  if (false) {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH AN UNPROCESSED TRACK WITH CONTENT CHANNEL FROM DPU AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Storage)")

    val interest_key: Interest = contentChannelStorageName call("/node/node2//type:track//paris-marathon", 1)

    // send interest for permissions from dpu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    dvu ? interest_key onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime3) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel")

    val interest_key2: Interest = keyChannelName call("/node/node2//type:track//paris-marathon", 1, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dpu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_key2)
    dvu ? interest_key2 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result (Key):  " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime4) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }

    }

    Thread.sleep(1000)
    subsection("Encryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))

  }


  // -----------------------------------------------------------------------------
  // ==== FETCH A PROCESSED TRACK WITH CONTENT CHANNEL FROM DPU ==================
  // -----------------------------------------------------------------------------

  if (false) {

    section("FETCH AN PROCESSED TRACK WITH CONTENT CHANNEL FROM DPU")

    Thread.sleep(1000)
    subsection("Content Channel (Processing)")

    val interest_key: Interest = contentChannelProcessingName call("/node/node2//type:track//paris-marathon", 2)

    // send interest for permissions from dpu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    dvu ? interest_key onComplete {
      // ... and receive content
      case Success(resultContent) => {
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime3) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }
    }

  }

  // -----------------------------------------------------------------------------
  // ==== FETCH A PROCESSED TRACK WITH CONTENT CHANNEL FROM DPU AND DECRYPT ======
  // -----------------------------------------------------------------------------

  if (true) {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH AN PROCESSED TRACK WITH CONTENT CHANNEL FROM DPU AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Processing)")

    val interest_key: Interest = contentChannelProcessingName call("/node/node2//type:track//paris-marathon", 2)

    // send interest for permissions from dpu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    dvu ? interest_key onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime3) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel")

    val interest_key2: Interest = keyChannelName call("/node/node2//type:track//paris-marathon", 2, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for key from dvu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_key2)
    dvu ? interest_key2 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result (Key):  " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime4) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }

    }

    Thread.sleep(1000)
    subsection("Encryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))

  }

  //============================================================================//
  //============================================================================//
  //============================================================================//

  // -----------------------------------------------------------------------------
  // ==== FETCH A PROCESSED TRACK WITH CONTENT CHANNEL THROUGH PROXY FROM DPU ====
  // -----------------------------------------------------------------------------

  if (false) {

    section("FETCH PERMISSIONS WITH ACCESS THROUGH PROXY FROM DSU")

    Thread.sleep(1000)
    subsection("Access Channel through proxy")

    val interest_key: Interest = proxyAccessChannelName call("/node/node2//type:track//paris-marathon")

    // send interest for permissions from dpu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    dvu ? interest_key onComplete {
      // ... and receive content
      case Success(resultContent) => {
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime3) + "ms")
        Monitor.monitor ! Monitor.Visualize()
        Thread.sleep(20000)
        nodes foreach {
          _.shutdown()
        }
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        Monitor.monitor ! Monitor.Visualize()
        nodes foreach {
          _.shutdown()
        }
      }
    }

  }

}

