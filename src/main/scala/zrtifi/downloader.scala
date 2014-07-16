package com.github.jmccrae.zrtifi

import java.net.{URL, MalformedURLException}
import java.io.File
import java.util.concurrent.Executors

object ZrtifiDownloader {

  val md5 = java.security.MessageDigest.getInstance("MD5")

  def startChain(urlString : String, backend : RDFBackend, processManager : ProcessManager) : String = {
    val url = try {
      new URL(urlString)
    } catch {
      case x : MalformedURLException => throw new ZrtifiDownloadException("Bad URL format", x)
    }
    if(url.getProtocol() != "http" && url.getProtocol() != "https" && url.getProtocol() != "ftp" && url.getProtocol() != "ftps") {
      throw new ZrtifiDownloadException("Unsupported URL protocol " + url.getProtocol())
    }
    if(url.getHost() == null || url.getHost() == "" || url.getHost() == "localhost" ||
      url.getHost() == "127.0.0.1" || url.getHost() == "[::1]") {
        throw new ZrtifiDownloadException("Bad host on URL " + url.getHost())
    }
    val report = "report/" + bytesToHex(md5.digest(urlString.getBytes()))

    backend.insertTriple(report, "", "<http://zrtifi.org/download>", "\"started\"")
    processManager.startThread(new DownloadExecutor(url, report, backend))

    return "/" + report
  }
}

class DownloadExecutor(url : URL, report : String, backend : RDFBackend) extends ProcessRunnable {
  def run(context : ProcessContext) {
    val tmpFile = context.createTempFile(url.getPath())
    var totalRead = 0

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
    backend.insertTriple(report, "", "<http://zrtifi.org/download>", "\"completed\"")
    context.chain(new SnifferRunnable(tmpFile))
  }
}


object bytesToHex {
  // Modified from StackOverflow answer by maybeWeCouldStealAVan
  private val hexArray = "0123456789ABCDEF".toCharArray()
  def apply(bytes : Array[Byte]) : String = {
    val hexChars = new Array[Char](bytes.length * 2)
    for(j <- 0 until bytes.length) {
        val v = bytes(j) & 0xFF;
        hexChars(j * 2) = hexArray(v >>> 4);
        hexChars(j * 2 + 1) = hexArray(v & 0x0F);
    }
    return new String(hexChars)
  }
}

case class ZrtifiDownloadException(msg : String = "", cause : Throwable = null) extends RuntimeException(msg, cause)
