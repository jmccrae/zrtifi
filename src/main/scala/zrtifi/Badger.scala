package zrtifi

import java.io.File
import java.util.concurrent.Executors

trait Badger {
  def possible(resource : File) : Boolean
  def start(resource : File) : Badging
}

trait Badging extends Runnable {
  def isFinished : Boolean
  def result : Option[File]
}

object GZipBadger extends Badger {
  def possible(resource : File) = resource.getPath().endsWith(".gz")
  def start(resource : File) = new GZipBadging(resource)
}

class GZipBadging(resource : File) extends Badging {
  private var finished = false
  private val tmpFile = File.createTempFile(resource.getName().dropRight(3), "")
  tmpFile.deleteOnExit() // For safety

  def run = {
    val in = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(resource))
    val out = new java.io.FileOutputStream(tmpFile)
    val buf = new Array[Byte](4096)
    var read = 0
    while({read = in.read(buf); read} > 0) {
      out.write(buf,0,read)
    }
    out.flush
    out.close
    in.close
    finished = true
  }
  def isFinished = finished
  def result = if(finished) {
    Some(tmpFile)
  } else {
    None
  }
  override def toString = if(finished) {
    "GZip complete"
  } else {
    "Ungzipping"
  }
}
