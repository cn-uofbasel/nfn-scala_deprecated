package filterAccess.runnables


import ccn.packet._
import com.typesafe.config.{Config, ConfigFactory}

import filterAccess.service.key.{ProxyKeyChannel, KeyChannelStorage, KeyChannel}
import filterAccess.service.permission.{ProxyPermissionChannel, PermissionChannelStorage, PermissionChannel}
import filterAccess.service.content.ContentChannelStorage
import filterAccess.service.content.ContentChannelFiltering
import filterAccess.service.content.ProxyContentChannel

import filterAccess.service.processing.track.distance._
import filterAccess.service.processing.track.maximum._

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
   *


                          {KeyChannel}
                   {PermissionChannel}
              {ContentChannel Storage}     {ContentChannel Filtering}
                               |                  |
                           +-------+          +-------+
                           |  dsu  |**********|  dcu  |
                           +-------+          +-------+
                                 *              *
                                  *            *
                                   *          *
                                   +-------+
                                ***|  dpu  | -- {ContentChannel Proxy}
                                *  +-------+    {PermissionChannel Proxy}
                +--------+*******      *        {KeyChannelProxy}
                |  ext   |             *
                +--------+*******      *
                 /              *  +--------+
     {...Channel Maximum}       ***| client | <--- Sends out Interests...
     {...Channel Distance}         +--------+          (CCN-only)


   *
   *
   **/


  // -----------------------------------------------------------------------------
  // ==== NETWORK SETUP ==========================================================
  // -----------------------------------------------------------------------------

  // network setup
  val dsu = LocalNodeFactory.forId(1, prefix=Option(CCNName("serviceprovider", "health", "storage")))
  val dcu = LocalNodeFactory.forId(2, prefix=Option(CCNName("serviceprovider", "health", "filtering")))
  val client = LocalNodeFactory.forId(3, prefix=Option(CCNName("personal", "device")), isCCNOnly = true)
  val dpu = LocalNodeFactory.forId(4, prefix=Option(CCNName("own", "machine")))
  val ext = LocalNodeFactory.forId(5, prefix=Option(CCNName("processing", "provider")))
  // NOTE: we adjusted the constructor of CCNName to take a custom prefix.
  // This is not incorporated in the original nfn-scala implementation.
  // Thus the previous five lines work just with our adjusted code base.
  val nodes = List(dsu, dcu, client, dpu, ext)
  dsu <~> dcu
  dcu <~> dpu
  dsu <~> dpu
  client <~> dpu
  ext <~> client
  ext <~> dpu

  // routing
  client.registerPrefixToNodes(dpu, List(dsu, dcu))
  ext.registerPrefixToNodes(dpu, List(dsu, dcu))
  dsu.registerPrefixToNodes(dpu, List(ext,client))
  dcu.registerPrefixToNodes(dpu, List(ext,client))

  section("network setup")

  println(
    """


                                 {KeyChannel}
                          {PermissionChannel}
                     {ContentChannel Storage}     {ContentChannel Filtering}
                                      |                  |
                                  +-------+          +-------+
                                  |  dsu  |**********|  dcu  |
                                  +-------+          +-------+
                                        *              *
                                         *            *
                                          *          *
                                          +-------+
                                       ***|  dpu  | -- {ContentChannel Proxy}
                                       *  +-------+    {PermissionChannel Proxy}
                       +--------+*******      *        {KeyChannelProxy}
                       |  ext   |             *
                       +--------+*******      *
                        /              *  +--------+
            {...Channel Maximum}       ***| client | <--- Sends out Interests...
            {...Channel Distance}         +--------+          (CCN-only)



    """)



  // -----------------------------------------------------------------------------
  // ==== SERVICE SETUP ==========================================================
  // -----------------------------------------------------------------------------

  section("service setup - storage and filtering")

  // service setup (permission channel)
  subsection("permission channel")
  val permissionChannelServ = new PermissionChannelStorage
  permissionChannelServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  permissionChannelServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dcu.publishServiceLocalPrefix(permissionChannelServ)
  val permissionChannelName = dcu.localPrefix.append(permissionChannelServ.ccnName)
  info(s"Permission channel installed on dcu: $permissionChannelName")
  info("Identity (public key) of permission channel service: " + permissionChannelServ.getPublicKey)

  // service setup (key channel - storage)
  subsection("key channel")
  val keyChannelServ = new KeyChannelStorage
  keyChannelServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  keyChannelServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dcu.publishServiceLocalPrefix(keyChannelServ)
  val keyChannelName = dcu.localPrefix.append(keyChannelServ.ccnName)
  info(s"Key channel installed on dcu: $keyChannelName")
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

  // service setup (content channel - filtering)
  subsection("content channel (filtering)")
  val contentChannelFilteringServ = new ContentChannelFiltering
  // contentChannelFilteringServ.setPublicKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")
  // contentChannelFilteringServ.setPrivateKey("MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")
  dcu.publishServiceLocalPrefix(contentChannelFilteringServ)
  val contentChannelFilteringName = dcu.localPrefix.append(contentChannelFilteringServ.ccnName)
  info(s"Content channel proxy installed on dcu: $contentChannelFilteringName")
  info("Identity (public key) of content channel service: " + contentChannelFilteringServ.getPublicKey)

  section("service setup - processing")

  // service setup (content channel - processing - distance)
  subsection("content channel (processing - distance)")
  val contentChannelDistanceServ = new ContentChannelDistance
  ext.publishServiceLocalPrefix(contentChannelDistanceServ)
  val contentChannelDistanceName = ext.localPrefix.append(contentChannelDistanceServ.ccnName)
  info(s"Content channel distance installed on ext: $contentChannelDistanceName")
  info("Identity (public key) of content channel service: " + contentChannelDistanceServ.getPublicKey)

  // service setup (key channel - processing - distance)
  subsection("key channel (processing - distance)")
  val keyChannelDistanceServ = new KeyChannelDistance
  ext.publishServiceLocalPrefix(keyChannelDistanceServ)
  val keyChannelDistanceName = ext.localPrefix.append(keyChannelDistanceServ.ccnName)
  info(s"Key channel distance installed on ext: $keyChannelDistanceName")
  info("Identity (public key) of key channel service: " + keyChannelDistanceServ.getPublicKey)

  // service setup (permission channel - processing - distance)
  subsection("permission channel (processing - distance)")
  val permissionChannelDistanceServ = new PermissionChannelDistance
  ext.publishServiceLocalPrefix(permissionChannelDistanceServ)
  val permissionChannelDistanceName = ext.localPrefix.append(permissionChannelDistanceServ.ccnName)
  info(s"Permission channel distance installed on ext: $permissionChannelDistanceName")
  info("Identity (public key) of permission channel service: " + permissionChannelDistanceServ.getPublicKey)


  // service setup (content channel - processing - maximum)
  subsection("content channel (processing - maximum)")
  val contentChannelMaximumServ = new ContentChannelMaximum
  ext.publishServiceLocalPrefix(contentChannelMaximumServ)
  val contentChannelMaximumName = ext.localPrefix.append(contentChannelMaximumServ.ccnName)
  info(s"Content channel maximum installed on ext: $contentChannelMaximumName")
  info("Identity (public key) of content channel service: " + contentChannelMaximumServ.getPublicKey)

  // service setup (key channel - processing - maximum)
  subsection("key channel (processing - maximum)")
  val keyChannelMaximumServ = new KeyChannelMaximum
  ext.publishServiceLocalPrefix(keyChannelMaximumServ)
  val keyChannelMaximumName = ext.localPrefix.append(keyChannelMaximumServ.ccnName)
  info(s"Key channel maximum installed on ext: $keyChannelMaximumName")
  info("Identity (public key) of key channel service: " + keyChannelMaximumServ.getPublicKey)

  // service setup (permission channel - processing - maximum)
  subsection("permission channel (processing - maximum)")
  val permissionChannelMaximumServ = new PermissionChannelMaximum
  ext.publishServiceLocalPrefix(permissionChannelMaximumServ)
  val permissionChannelMaximumName = ext.localPrefix.append(permissionChannelMaximumServ.ccnName)
  info(s"Permission channel maximum installed on ext: $permissionChannelMaximumName")
  info("Identity (public key) of permission channel service: " + permissionChannelMaximumServ.getPublicKey)

  // -----------------------------------------------------------------------------
  // ==== PROXY SETUP ============================================================
  // -----------------------------------------------------------------------------

  section("proxy setup")

  // service setup (content channel)
  subsection("content channel")
  val proxyContentChannelServ = new ProxyContentChannel
  dpu.publishServiceLocalPrefix(proxyContentChannelServ)
  val proxyContentChannelName = dpu.localPrefix.append(proxyContentChannelServ.ccnName)
  info(s"Content channel proxy installed on dpu: $proxyContentChannelName")

  // service setup (permission channel)
  subsection("permission channel")
  val proxyPermissionChannelServ = new ProxyPermissionChannel
  dpu.publishServiceLocalPrefix(proxyPermissionChannelServ)
  val proxyPermissionChannelName = dpu.localPrefix.append(proxyPermissionChannelServ.ccnName)
  info(s"Content channel proxy installed on dpu: $proxyPermissionChannelName")

  // service setup (key channel)
  subsection("key channel")
  val proxyKeyChannelServ = new ProxyKeyChannel
  dpu.publishServiceLocalPrefix(proxyKeyChannelServ)
  val proxyKeyChannelName = dpu.localPrefix.append(proxyKeyChannelServ.ccnName)
  info(s"Content channel proxy installed on dpu: $proxyKeyChannelName")


  //============================================================================//
  //============================================================================//
  //============================================================================//


  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSIONS FROM DSU =============================================
  // -----------------------------------------------------------------------------

  def filtering1: Unit = {

    section("FETCH PERMISSIONS FROM DSU")

    Thread.sleep(1000)

    val interest_permissions: Interest = permissionChannelName call ("/john/doe/track/stadtlauf2015")

    // send interest for permissions from dpu...
    val startTime2 = System.currentTimeMillis
    info("Send interest: " + interest_permissions)
    client ? interest_permissions onComplete {
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


  def filtering2: Unit = {

    section("FETCH KEY FROM DSU")

    Thread.sleep(1000)

    val interest_key: Interest = keyChannelName call("/john/doe/track/paris-marathon", 2, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from client...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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



  def filtering3: Unit = {

    var permissionData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH PERMISSION AS WELL AS KEY FROM DSU AND PERFORM ENCRYPTION")

    Thread.sleep(1000)
    subsection("Access Channel")

    val interest_permissions: Interest = permissionChannelName call ("/john/doe/track/stadtlauf2015")

    // send interest for permissions from dvu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_permissions)
    client ? interest_permissions onComplete {
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

    val interest_key: Interest = keyChannelName call("/john/doe/track/stadtlauf2015", 0, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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
  // ==== FETCH AN UNFILTERED TRACK WITH CONTENT CHANNEL FROM DSU AND DECRYPT ===
  // -----------------------------------------------------------------------------

  def filtering4: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH AN UNFILTERED TRACK WITH CONTENT CHANNEL FROM DPU AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Storage)")

    val interest_key: Interest = contentChannelStorageName call("/john/doe/track/paris-marathon", 1)

    // send interest for permissions from dvu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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

    val interest_key2: Interest = keyChannelName call("/john/doe/track/paris-marathon", 1, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_key2)
    client ? interest_key2 onComplete {
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
  // ==== FETCH A FILTERED TRACK WITH CONTENT CHANNEL FROM DCU ==================
  // -----------------------------------------------------------------------------

  def filtering5: Unit = {

    section("FETCH AN FILTERED TRACK WITH CONTENT CHANNEL FROM DPU")

    Thread.sleep(1000)
    subsection("Content Channel (Filtering)")

    val interest_key: Interest = contentChannelFilteringName call("/john/doe/track/paris-marathon", 2)

    // send interest for permissions from dvu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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
  // ==== FETCH A FILTERED TRACK WITH CONTENT CHANNEL FROM DCU AND DECRYPT ======
  // -----------------------------------------------------------------------------

  def filtering6: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH AN FILTERED TRACK WITH CONTENT CHANNEL FROM DPU AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Filtering)")

    val interest_key: Interest = contentChannelFilteringName call("/john/doe/track/paris-marathon", 2)

    // send interest for permissions from dvu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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

    // TODO -- Uncommenting the following line causes timeouts? why? Logging might be meshed up without that sleep..
    // Thread.sleep(1000)
    subsection("Key Channel")

    val interest_key2: Interest = keyChannelName call("/john/doe/track/paris-marathon", 2, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for key from dvu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_key2)
    client ? interest_key2 onComplete {
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
    subsection("Decryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))

  }

  //============================================================================//
  //============================================================================//
  //============================================================================//

  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSION DATA AND KEY THROUGH DPU AND DECRYPT ==================
  // -----------------------------------------------------------------------------

  def filtering7: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH PERMISSION DATA AND KEY THROUGH DPU AND DECRYPT")

    Thread.sleep(1000)
    subsection("Permission Channel through dpu")

    val interest_key: Interest = proxyPermissionChannelName call("/john/doe/track/paris-marathon")

    // send interest for permissions from dvu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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

    val interest_key2: Interest = proxyKeyChannelName call("/john/doe/track/paris-marathon", 0, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for key from dvu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_key2)
    client ? interest_key2 onComplete {
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
    subsection("Decryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))

  }

  // -----------------------------------------------------------------------------
  // ==== FETCH FILTERED DATA AND KEY THROUGH DPU AND DECRYPT ===================
  // -----------------------------------------------------------------------------

  def filtering8: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."

    section("FETCH FILTERED DATA AND KEY THROUGH DPU AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Filtering)")

    val interest_key: Interest = proxyContentChannelName call("/john/doe/track/paris-marathon", 2)

    // send interest for permissions from dvu...
    val startTime3 = System.currentTimeMillis
    info("Send interest: " + interest_key)
    client ? interest_key onComplete {
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

    // TODO -- Uncommenting the following line causes timeouts? why? Logging might be meshed up without that sleep..
    // Thread.sleep(1000)

    subsection("Key Channel")

    // TODO -- change to proxy
    val interest_key2: Interest = proxyKeyChannelName call("/john/doe/track/paris-marathon", 2, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for key from dvu...
    val startTime4 = System.currentTimeMillis
    info("Send interest: " + interest_key2)
    client ? interest_key2 onComplete {
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
    subsection("Decryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))

  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  // -----------------------------------------------------------------------------
  // ==== FETCH PROCESSED CONTENT (LENGTH OF A TRACK) ============================
  // -----------------------------------------------------------------------------

  def processing1: Unit = {

    var contentData: String = "to be fetched..."

    section("FETCH PROCESSED CONTENT (LENGTH OF A TRACK)")

    Thread.sleep(1000)
    subsection("Content Channel (Processing - Distance)")

    val interest: Interest = contentChannelDistanceName call("/john/doe/track/jungfraujoch@/own/machine", 1)

    // send interest for permissions from dvu...
    val startTime = System.currentTimeMillis
    info("Send interest: " + interest)
    client ? interest onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime) + "ms")
        //Monitor.monitor ! Monitor.Visualize()
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
  // ==== FETCH SYNTHESIZED KEY FOR PROCESSED DATA (LENGTH OF A TRACK) ===========
  // -----------------------------------------------------------------------------

  def processing2: Unit = {

    var keyData: String = "to be fetched..."

    section("FETCH SYNTHESIZED KEY FOR PROCESSED DATA (LENGTH OF A TRACK)")

    Thread.sleep(1000)
    subsection("Key Channel (Processing - Distance)")

    val interest: Interest = keyChannelDistanceName call("/john/doe/track/stadtlauf2015@/own/machine", 1, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime = System.currentTimeMillis
    info("Send interest: " + interest)
    client ? interest onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime) + "ms")
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
    subsection("Decryption")
    info("Decrypted:     " + privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="))

  }


  // -----------------------------------------------------------------------------
  // ==== FETCH PROCESSED CONTENT (LENGTH OF A TRACK) WITH KEY AND DECRYPT =======
  // -----------------------------------------------------------------------------

  def processing3: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."


    section("FETCH PROCESSED CONTENT (LENGTH OF A TRACK) WITH KEY AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Processing - Distance)")

    val interest1: Interest = contentChannelDistanceName call("/john/doe/track/stadtlauf2015@/own/machine", 1)

    // send interest for permissions from dvu...
    val startTime1 = System.currentTimeMillis
    info("Send interest: " + interest1)
    client ? interest1 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime1) + "ms")
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        // Monitor.monitor ! Monitor.Visualize()
        // nodes foreach {
        // _.shutdown()
        //}
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel (Processing - Distance)")

    val interest2: Interest = keyChannelDistanceName call("/john/doe/track/stadtlauf2015@/own/machine", 1, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime2 = System.currentTimeMillis
    info("Send interest: " + interest2)
    client ? interest2 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime2) + "ms")
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
    subsection("Decryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))
    // info("Decrypted:     " + privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="))

  }


  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSIONS TO PROCESSED CONTENT WITH KEY AND DECRYPT ============
  // -----------------------------------------------------------------------------

  def processing4: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."


    section("FETCH PERMISSIONS TO PROCESSED CONTENT WITH KEY AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Processing - Distance)")

    val interest1: Interest = permissionChannelDistanceName call("/john/doe/track/stadtlauf2015@/own/machine")

    // send interest for permissions from dvu...
    val startTime1 = System.currentTimeMillis
    info("Send interest: " + interest1)
    client ? interest1 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime1) + "ms")
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        // Monitor.monitor ! Monitor.Visualize()
        // nodes foreach {
        // _.shutdown()
        //}
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel (Processing - Distance)")

    val interest2: Interest = keyChannelDistanceName call("/john/doe/track/stadtlauf2015@/own/machine", 0, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime2 = System.currentTimeMillis
    info("Send interest: " + interest2)
    client ? interest2 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime2) + "ms")
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
    subsection("Decryption")
    // info("Decrypted (sym): " + privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="))
    info("Decrypted:       " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  // -----------------------------------------------------------------------------
  // ==== FETCH PROCESSED CONTENT (MAX OF TWO TRACKS) ============================
  // -----------------------------------------------------------------------------

  def processing5: Unit = {

    var contentData: String = "to be fetched..."

    section("FETCH PROCESSED CONTENT (MAX OF TWO TRACKS)")

    Thread.sleep(1000)
    subsection("Content Channel (Processing - Maximum)")

    val interest: Interest = contentChannelMaximumName call("/john/doe/track/paris-marathon@/own/machine", "/john/doe/track/stadtlauf2015@/own/machine")
    // TODO -- try with "jungfraujoch" instead of "stadtlauf2015"

    // send interest for permissions from dvu...
    val startTime = System.currentTimeMillis
    info("Send interest: " + interest)
    client ? interest onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime) + "ms")
        //Monitor.monitor ! Monitor.Visualize()
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
  // ==== FETCH SYNTHESIZED KEY FOR PROCESSED DATA (MAX OF TWO TRACKS) ===========
  // -----------------------------------------------------------------------------

  def processing6: Unit = {

    var keyData: String = "to be fetched..."

    section("FETCH SYNTHESIZED KEY FOR PROCESSED DATA (MAX OF TWO TRACKS)")

    Thread.sleep(1000)
    subsection("Key Channel (Processing - Maximum)")

    val interest: Interest = keyChannelMaximumName call("/john/doe/track/stadtlauf2015@/own/machine", "/john/doe/track/paris-marathon@/own/machine", 1, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime = System.currentTimeMillis
    info("Send interest: " + interest)
    client ? interest onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime) + "ms")
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
    subsection("Decryption")
    info("Decrypted:     " + privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="))

  }

  // -----------------------------------------------------------------------------
  // ==== FETCH PROCESSED CONTENT (MAX OF TWO TRACKS) WITH KEY AND DECRYPT =======
  // -----------------------------------------------------------------------------

  def processing7: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."


    section("FETCH PROCESSED CONTENT (MAX OF TWO TRACKS) WITH KEY AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Processing - Maximum)")

    val interest1: Interest = contentChannelMaximumName call("/john/doe/track/stadtlauf2015@/own/machine", "/john/doe/track/paris-marathon@/own/machine")

    // send interest for permissions from dvu...
    val startTime1 = System.currentTimeMillis
    info("Send interest: " + interest1)
    client ? interest1 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime1) + "ms")
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        // Monitor.monitor ! Monitor.Visualize()
        // nodes foreach {
        // _.shutdown()
        //}
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel (Processing - Maximum)")

    val interest2: Interest = keyChannelMaximumName call("/john/doe/track/stadtlauf2015@/own/machine", "/john/doe/track/paris-marathon@/own/machine", 1, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime2 = System.currentTimeMillis
    info("Send interest: " + interest2)
    client ? interest2 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime2) + "ms")
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
    subsection("Decryption")
    info("Decrypted:     " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))
    // info("Decrypted:     " + privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="))

  }

  // -----------------------------------------------------------------------------
  // ==== FETCH PERMISSIONS TO PROCESSED CONTENT WITH KEY AND DECRYPT ============
  // -----------------------------------------------------------------------------

  def processing8: Unit = {

    var contentData: String = "to be fetched..."
    var keyData: String = "to be fetched..."


    section("FETCH PERMISSIONS TO PROCESSED CONTENT (MAX OF TWO TRACKS) WITH KEY AND DECRYPT")

    Thread.sleep(1000)
    subsection("Content Channel (Processing - Maximum)")

    val interest1: Interest = permissionChannelMaximumName call("/john/doe/track/stadtlauf2015@/own/machine", "/john/doe/track/paris-marathon@/own/machine")

    // send interest for permissions from dvu...
    val startTime1 = System.currentTimeMillis
    info("Send interest: " + interest1)
    client ? interest1 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        contentData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime1) + "ms")
      }
      // ... but do not get content
      case Failure(e) => {
        info("No content received.")
        // Monitor.monitor ! Monitor.Visualize()
        // nodes foreach {
        // _.shutdown()
        //}
      }
    }

    Thread.sleep(1000)
    subsection("Key Channel (Processing - Maximum)")

    val interest2: Interest = keyChannelMaximumName call("/john/doe/track/stadtlauf2015@/own/machine", "/john/doe/track/paris-marathon@/own/machine", 0, "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==")

    // send interest for permissions from dvu...
    val startTime2 = System.currentTimeMillis
    info("Send interest: " + interest2)
    client ? interest2 onComplete {
      // ... and receive content
      case Success(resultContent) => {
        keyData = new String(resultContent.data)
        info("Result:        " + new String(resultContent.data))
        info("Time:          " + (System.currentTimeMillis - startTime2) + "ms")
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
    subsection("Decryption")
    // info("Decrypted (sym): " + privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ=="))
    info("Decrypted:       " + symDecrypt(contentData, privateDecrypt(keyData, "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmmgXeOVTP044WC8S2toUy5o64DbXUKTdQe4dOgzKzrm1Ps+q9jztU105+Uk3WhOqoi7Ldvwefivjl24kVz034wIDAQABAkAecJbwBoW63TjOablV29htqyIgQa+A/n+AF+k7IHp69mDE7CtlikW4bDQXsaPVw1Sp18UhnZUJgfEFCjGPmimBAiEA/YcXjwvgAL/bfvsOwMWg44LwjY4g/WXdVHxLp4VXnksCIQCb6Y2e+P4RdOAdgvMP3+riIBs7B2U4u0eIyR6NbaRtyQIgMBu2aLqEIyBE8m+JeSMHSKTMKNBTikIOIb4ETSGMYskCIDQzy8Y5ih/gKRXYfXeIOoXByDxIapzHH9lttXwXBOH5AiBLTG6tCPaSz3DdslndvdK6dfy8Beg0iV1QdiqyAYe/fQ==")))
  }



  // -----------------------------------------------------------------------------
  // ==== UNCOMMENT A TEST... ====================================================
  // -----------------------------------------------------------------------------

  // NOTE: Uncommenting more than one test at once causes messed up output. Further, some tests might fail
  // because each test shuts down the nodes after completion.


  ///// STORAGE AND FILTERING /////

  // filtering1      // FETCH PERMISSIONS FROM DSU
  // filtering2      // FETCH KEY FROM DSU
  // filtering3      // FETCH PERMISSION AS WELL AS KEY FROM DSU AND PERFORM ENCRYPTION
  // filtering4      // FETCH AN UNFILTERED TRACK WITH CONTENT CHANNEL FROM DSU AND DECRYPT

  // filtering5      // FETCH A FILTERED TRACK WITH CONTENT CHANNEL FROM DCU
  // filtering6      // FETCH A FILTERED TRACK WITH CONTENT CHANNEL FROM DCU AND DECRYPT

  // filtering7      // FETCH PERMISSION DATA AND KEY THROUGH DPU AND DECRYPT
  // filtering8      // FETCH FILTERED DATA AND KEY THROUGH DPU AND DECRYPT


  ///// PROCESSING /////

  // processing1    // FETCH PROCESSED CONTENT (LENGTH OF A TRACK)
  // processing2    // FETCH SYNTHESIZED KEY FOR PROCESSED DATA (LENGTH OF A TRACK)
  // processing3    // FETCH PROCESSED CONTENT (LENGTH OF A TRACK) WITH KEY AND DECRYPT
  // processing4    // FETCH PERMISSIONS TO PROCESSED CONTENT WITH KEY AND DECRYPT

  // processing5    // FETCH PROCESSED CONTENT (MAX OF TWO TRACKS)
  // processing6    // FETCH SYNTHESIZED KEY FOR PROCESSED DATA (MAX OF TWO TRACKS)
  // processing7    // FETCH PROCESSED CONTENT (MAX OF TWO TRACKS) WITH KEY AND DECRYPT
  // processing8    // FETCH PERMISSIONS TO PROCESSED CONTENT (MAX OF TWO TRACKS) WITH KEY AND DECRYPT

}