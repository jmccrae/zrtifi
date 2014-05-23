package zrtifi

import java.io.File
import java.util.concurrent.Executors

object Downloader {
  def download(url : String) : Badging = new DownloadBadging(url)
}

class DownloadBadging(urlStr : String) extends Badging {
  def fileSuffix(str : String) = if(str.contains(".")) {
    str.substring(str.lastIndexOf("."))
  } else {
    ""
  }
  private var finished = false
  private val url = new java.net.URL(urlStr)
  private val tmpFile = File.createTempFile(new File(url.getPath()).getName(), fileSuffix(url.getPath()))
  private var totalRead = 0

  def run {
    val in = url.openStream()
    val out =  new java.io.FileOutputStream(tmpFile)
    val buf = new Array[Byte](4096)
    var read = 0
    while({read = in.read(buf); read} > 0) {
      out.write(buf,0,read)
      totalRead += read
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
    "Download completed"
  } else {
    "Downloading: %d bytes" format (totalRead)
  }
}
