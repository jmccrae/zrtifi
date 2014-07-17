package com.github.jmccrae.zrtifi

import eu.monnetproject.framework.services.Services
import java.io.File
import java.io.FileInputStream
import scala.collection.JavaConversions._

trait Sniffer {
  def isInFormat(fileName : String, firstKilobyte : Array[Byte]) : Boolean
  def chain() : String
}

object SnifferRunnable {
  val sniffers = Services.getAll(classOf[Sniffer]).toList

}

class SnifferRunnable(file : File) extends ProcessRunnable {
  import SnifferRunnable._

  def run(context : ProcessContext) {
    val fileName = file.getName()
    val firstKilobyte = new Array[Byte](1000)
    val fileInputStream = new FileInputStream(file)
    fileInputStream.read(firstKilobyte)
    fileInputStream.close()
    val validSniffers = sniffers.filter(_.isInFormat(fileName, firstKilobyte))
    println("Found %d/%d sniffers " format (validSniffers.size, sniffers.size))
    chainSniffer(validSniffers, file, context)  
  }

  private def chainSniffer(validSniffers : List[Sniffer], file : File, context : ProcessContext) {
    validSniffers match {
      case Nil => 
      case sniffer :: sniffers => {
        ExecRunnable.resolveExecutable(sniffer.chain(), file) match {
          case Some(runnable) => {
            System.err.println("chaining")
            context.chain(runnable)
          }
          case None => System.err.println("Could not resolve chain: " + sniffer.chain())
        }
        chainSniffer(sniffers, file, context)
      }
    }
  }
}


/// Standard sniffers

class ZIPSniffer extends Sniffer {
  def isInFormat(fileName : String, firstKilobyte : Array[Byte]) = fileName.endsWith(".zip")
  def chain() : String = "unzip"
}

class GZSniffer extends Sniffer {
  def isInFormat(fileName : String, firstKilobyte : Array[Byte]) = fileName.endsWith(".gz")
  def chain() : String = "gzip"
}

class TarSniffer extends Sniffer {
  def isInFormat(fileName : String, firstKilobyte : Array[Byte]) = fileName.endsWith(".tar")
  def chain() : String = "tar"
}
