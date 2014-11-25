package bytecode

import java.io.{BufferedOutputStream, FileOutputStream, File}

import nfn.service.{WordCountService, NFNService}
import org.scalatest.{Matchers, FlatSpec}
import scala.util.{Failure, Success}


class BytecodeLoaderTest extends FlatSpec with Matchers {

  "The bytecode of a compiled class (WordCountService)" should "be loaded from its bytecode stored on the filesystem and be instantiated back to a corresponding class" in {
    val tempBcFile = new File(s"/tmp/BytecodeLoaderTest${System.currentTimeMillis()}")

    val fw = new BufferedOutputStream(new FileOutputStream(tempBcFile))
    try {
      val cl = new WordCountService()
      val bc: Array[Byte] = BytecodeLoader.byteCodeForClass(cl).get
      fw.write(bc)
      fw.flush()
      BytecodeLoader.loadClass[NFNService](tempBcFile.getCanonicalPath, cl.getClass.getName).get should be(an[NFNService])
    } finally {
      fw.close()
      tempBcFile.delete()
    }
  }
}
