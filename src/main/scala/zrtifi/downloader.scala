package com.github.jmccrae.zrtifi

import java.net.{URL, MalformedURLException}
import java.io.File
import java.util.concurrent.Executors
import ZrtifiSettings._

object ZrtifiDownloader {

  val md5 = java.security.MessageDigest.getInstance("MD5")

  private def escapeLiteral(literal : String) = "\"" + (
    for(c <- literal) yield {
      if(c > 128) {
        "\\u%04x" format c.toInt
      } else {
        c
      }
    }).mkString("") + "\""


  def startChain(urlString : String, backend : RDFBackend, processManager : ProcessManager, name : Option[String]) : String = {
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

    backend.removeTriples(report)
    name match {
      case Some(x) if x != "" => backend.insertTriple(report, "", "<http://www.w3.org/2000/01/rdf-schema#label>", escapeLiteral(x))
      case _ => backend.insertTriple(report, "", "<http://www.w3.org/2000/01/rdf-schema#label>", "\"Dataset from %s\"" format url.getHost())
    }
    backend.insertTriple(report, "", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://www.w3.org/ns/dcat#Dataset>")
    backend.insertTriple(report, "", "<http://www.w3.org/ns/dcat#distribution>", "<%s%s#Distribution>" format (BASE_NAME, report))
    backend.insertTriple(report, "Distribution", "<http://www.w3.org/ns/dcat#downloadURL>", "<%s>" format urlString)
    backend.insertTriple(report, "", "<%svalidationStatus>" format ZRTIFI_ONTOLOGY, "<%sinProgress>" format ZRTIFI_ONTOLOGY)

    processManager.startThread(new DownloadExecutor(url, report, backend), report)

    return report
  }
}

class DownloadExecutor(url : URL, report : String, backend : RDFBackend) extends ProcessRunnable {
  def run(context : ProcessContext) {
    val tmpFile = context.createTempFile(url.getPath())
    var totalRead = 0l

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
    backend.insertTriple(report, "", "<%sstatus>" format ZRTIFI_ONTOLOGY, "<%ssuccess>" format ZRTIFI_ONTOLOGY)
    backend.insertTriple(report, "Distribution", "<http://www.w3.org/ns/dcat#byteSize>",
      "\"%d\"^^<http://www.w3.org/2001/XMLSchema#decimal>" format totalRead)
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
