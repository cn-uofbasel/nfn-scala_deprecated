package nfn.service

import java.io.{File, FileOutputStream}

import akka.actor.ActorRef
import bytecode.BytecodeLoader
import ccn.packet._
import com.typesafe.scalalogging.slf4j.Logging
import nfn.NFNApi

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}


object NFNServiceLibrary extends Logging {
  def nfnPublishService(serv: NFNService, prefix: Option[CCNName], ccnWorker: ActorRef) = {
    def pinnedData = "pinnedfunction".getBytes

    def byteCodeData(serv: NFNService):Array[Byte] = {
      BytecodeLoader.byteCodeForClass(serv).getOrElse {
        logger.error(s"nfnPublush: No bytecode found for unpinned service $serv")
        pinnedData
      }
    }

    val serviceContent =
      if(serv.pinned) pinnedData
      else byteCodeData(serv)

    val content = Content(
      prefix.map(_.append(serv.ccnName)).getOrElse(serv.ccnName),
      serviceContent,
      MetaInfo.empty
    )

    logger.debug(s"nfnPublish: Adding ${content.name} (size=${serviceContent.size}) to cache")
    ccnWorker ! NFNApi.AddToCCNCache(content)
  }
}



object Main {

  def bytecodeLoading = {
    val jarfile = "/Users/basil/Dropbox/uni/master_thesis/code/testservice/target/scala-2.10/testservice_2.10-0.1-SNAPSHOT.jar"
    val service = BytecodeLoader.loadClass[NFNService](jarfile, "NFNServiceTestImpl")
  }

  def findLibraryFunctionWithReflection = {
    def getTypeTag[T: ru.TypeTag](obj: T) = ru.typeTag[T]

    val tpe = getTypeTag(NFNServiceLibrary).tpe

    val methods: List[String] =
      tpe.declarations.filter(decl =>
        decl.isMethod && decl.name.toString != "<init>"
      ).map( methodDecl => {
        def parseTypeString(tpe: String): String = {
          val splitted = tpe.substring(1, tpe.size).split("\\)")
          val params:List[String] = splitted(0).split(", ").map(param => param.substring(param.indexOf(": ")+2, param.size)).toList
          val ret = splitted(1).trim
          s"${params.mkString("/")}/r$ret"
        }
        val name = methodDecl.name.toString
        val tpes = parseTypeString(methodDecl.typeSignature.toString)
        //        typeSignature.map( (tpe: ru.Type) => {
        ////          tpe.toString
        ////        }
        ////        )
        s"/$name/$tpes"
      }
        ).toList

    println(methods.mkString("\n"))
  }
}

