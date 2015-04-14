package filterAccess.runnables


import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}

import filterAccess.service.key.KeyChannel
import filterAccess.service.access.AccessChannel
import filterAccess.service.content.ContentChannelStorage
import filterAccess.service.content.ContentChannelProcessing

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
   *  Network with dsu, dpu and dvu.
   *  Service "ContentChannel" on dpu.
   *  Services "KeyChannel" and "AccessChannel" on dsu.
   *

           {track.KeyChannel}
           {track.AccessChannel}     {track.ContentChannel}
                      |                  |
                  +-------+          +-------+
                  |  dsu  |**********|  dpu  |
                  +-------+          +-------+
                        *              *
                         *            *
                          *          *
                           +-------+
                           |  dvu  | <--- Sends out Interests...
                           +-------+           (CCN Only)

   *
   * DATA:
   *  /<dsu_prefix>//type:track//stadtlauf15/...
   *  /<dsu_prefix>//type:track//paris-marathon/...
   *  /<dsu_prefix>//type:track//jungfraujoch/...
   *
   * SCENARIO:
   *  tbd
   *
   * */


  // -----------------------------------------------------------------------------
  // ==== NETWORK SETUP ==========================================================
  // -----------------------------------------------------------------------------

  // network setup
  val dsu = LocalNodeFactory.forId(1)
  val dpu = LocalNodeFactory.forId(2)
  val dvu = LocalNodeFactory.forId(3, isCCNOnly = true)
  val nodes = List(dsu, dpu, dvu)
  dsu <~> dpu
  dpu <~> dvu
  dvu <~> dsu



  // -----------------------------------------------------------------------------
  // ==== SERVICE SETUP ==========================================================
  // -----------------------------------------------------------------------------

  section("service setup")

  // service setup (access/permission channel)
  subsection("access/permission channel")
  val accessChannelServ = new AccessChannel
  accessChannelServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  accessChannelServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dpu.publishServiceLocalPrefix(accessChannelServ)
  val accessChannelName = dpu.localPrefix.append(accessChannelServ.ccnName)
  info(s"Access channel installed on dpu: $accessChannelName")
  info("Identity (public key) of access chanel service: " + accessChannelServ.getPublicKey)

  // service setup (key channel)
  subsection("key channel")
  val keyChannelServ = new KeyChannel
  keyChannelServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  keyChannelServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dpu.publishServiceLocalPrefix(keyChannelServ)
  val keyChannelName = dpu.localPrefix.append(keyChannelServ.ccnName)
  info(s"Key channel installed on dpu: $keyChannelName")
  info("Identity (public key) of access chanel service: " + keyChannelServ.getPublicKey)

  // service setup (content channel - storage)
  subsection("content channel (storage)")
  val contentChannelStorageServ = new ContentChannelStorage
  // contentChannelStorageServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  // contentChannelStorageServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dsu.publishServiceLocalPrefix(contentChannelStorageServ)
  val contentChannelStorageName = dsu.localPrefix.append(contentChannelStorageServ.ccnName)
  info(s"Content channel (storage) installed on dsu: $contentChannelStorageName")
  info("Identity (public key) of access chanel service: " + contentChannelStorageServ.getPublicKey)

  // service setup (content channel - processing)
  subsection("content channel (processing)")
  val contentChannelProcessingServ = new ContentChannelProcessing
  // contentChannelProcessingServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  // contentChannelProcessingServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dpu.publishServiceLocalPrefix(contentChannelProcessingServ)
  val contentChannelProcessingName = dpu.localPrefix.append(contentChannelProcessingServ.ccnName)
  info(s"Content channel (processing) installed on dpu: $contentChannelProcessingName")
  info("Identity (public key) of access chanel service: " + contentChannelProcessingServ.getPublicKey)


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

  if (false) {

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
}

