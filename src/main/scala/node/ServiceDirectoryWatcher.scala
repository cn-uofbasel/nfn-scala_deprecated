package node

import java.io.File
import ccn.packet.CCNName
import myutil.IOHelper
import nfn.service.NFNService

import scala.collection._

trait ServiceDirectoryWatcher {
  val dir = "./service-library"
  val dirFile = new File(dir)
  
  val checkedFiles: mutable.Set[String] = mutable.Set()
  
  dirFile.listFiles() foreach { file =>
    if(!checkedFiles.contains(file.getCanonicalPath)) {
      val name = CCNName(file.getName.split("_").toList, None)
      val data = IOHelper.readByteArrayFromFile(file)
    }
  }
  
  def publishServiceLocalPrefix(serv: NFNService)
}
