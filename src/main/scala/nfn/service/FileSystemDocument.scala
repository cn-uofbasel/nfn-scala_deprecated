package nfn.service

import java.nio.file.{Files, Paths}

import ccn.packet.{Content, CCNName}

/**
 * Created by rgasser on 02.05.15.
 */
object FileSystemDocument {
  def documentWithPath(path : String, prefix: CCNName) = Content(prefix.append("docs", "path"), Files.readAllBytes(Paths.get(path)));
}
