package com.github.jmccrae.zrtifi

import java.io.File
import scala.collection.immutable.Queue
import org.apache.commons.exec.{CommandLine, DefaultExecutor, DefaultExecuteResultHandler, ExecuteWatchdog, PumpStreamHandler, LogOutputStream}
import ZrtifiSettings._

class ProcessManager(dataModel : RDFBackend) {
  private val processes = collection.mutable.Map[String,Thread]()
  private val folders = collection.mutable.Map[String,File]()
  private val statuses = collection.mutable.Map[String,String]()
  private val rdfIDs = collection.mutable.Map[String,String]()
  
  private def deleteRecursively(file : File) {
    if(file.isFile()) {
      file.delete()
    } else if(file.isDirectory()) {
      for(file <- file.listFiles()) {
        deleteRecursively(file)
      }
      file.delete()
    } else {
      System.err.println("Neither file nor directory " + file.getName())
    }
  }

  private def cleanUpProcess(id : String) {
    statuses += id -> "Clean up"
    processes.remove(id)
    folders.get(id) match {
      case Some(dir) => deleteRecursively(dir)
      case None => 
    }
    folders.remove(id)
    statuses.remove(id)
    rdfIDs.remove(id)
  }

  def folderForProcess(id : String) = folders.get(id) match {
    case Some(file) => file
    case None => {
      val tmpFile = new File(new File(System.getProperty("java.io.tmpdir")), "zrtifi" + id)
      folders += id -> tmpFile
      tmpFile.mkdirs()
      tmpFile
    }
  }

  def createTempFile(id : String, path : String) = {
    val tmpFileName = new File(path).getName() match {
      case "" => math.abs(util.Random.nextInt).toHexString
      case x => x
    }
    new File(folderForProcess(id), tmpFileName)
  }

  def startThread(runnable : ProcessRunnable, rdfID : String) : String = {
    val id = math.abs(util.Random.nextInt).toString
    val context = new ProcessContextImpl(id)
    val thread = new Thread(new Runnable {
      def run {
        try {
          statuses += id -> ("Running %s" format runnable.getClass.getName)
          System.err.println("Running %s" format runnable.getClass.getName)
          runnable.run(context)
          /*this.synchronized {
            wait(10000)
          }*/
          var next = context.next
          while(next != None) {
            statuses += id -> ("Running %s" format next.get.getClass.getName)
            System.err.println("Running %s" format next.get.getClass.getName)
            next.get.run(context)
            next = context.next
          }
        } finally {
          cleanUpProcess(id)
        }
      }
    })
    rdfIDs += id -> rdfID
    processes += id -> new Thread(thread)
    thread.start
    return id
  }

  private class ProcessContextImpl(id : String) extends ProcessContext {
    var chains = Queue[ProcessRunnable]()
    def tempFolder = folderForProcess(id)
    def createTempFile(path : String) = ProcessManager.this.createTempFile(id, path)
    def chain(runnable : ProcessRunnable) { chains = chains.enqueue(runnable) }
    def next = {
      if(chains.isEmpty) {
        None
      } else {
        chains.dequeue match {
          case (v, c) => {
            chains = c
            Some(v)
          }
        }
      }
    }
    def addTriple(frag : String, prop : String, obj : String) {
      rdfIDs.get(id) match {
        case Some(rdfID) => dataModel.insertTriple(rdfID, frag, prop, obj)
        case None => System.err.println("No rdfID for this process! (Should not happen)")
      }
    }
  }

  def statusHTML : String = {
    "<table class='process_status'><tr><th>Process ID</th><th>Process Status</th></tr>%s</table>" format (processes.map {
      case (id,y) =>
        "<tr><td>%s</td><td>%s</td></tr>" format (id, statuses.getOrElse(id, ""))
    } mkString "")
  }
}

trait ProcessRunnable {
  def run(context : ProcessContext) : Unit
}

trait ProcessContext {
  def tempFolder : File
  def createTempFile(path : String) : File
  def chain(runnable : ProcessRunnable) : Unit
  def next : Option[ProcessRunnable]
  def addTriple(frag : String, property : String, obj : String) : Unit
}


class ExecRunnable(command : String, args : String*) extends ProcessRunnable {
  val tripleRegex = "<(#[^>]*)?>\\s+(<[^>]+>)\\s+(\".*?(?<!\\\\)\"(|@[\\w-]+|\\^\\^<[^>]+>)|<[^>]+>)\\s*\\.\\s*".r

  def run(context : ProcessContext) {
    val cmdLine = new CommandLine(command)
    for(arg <- args) {
      cmdLine.addArgument(arg)
    }
    val handler = new DefaultExecuteResultHandler()
    val watchdog = new ExecuteWatchdog(EXTERNAL_PROCESS_TIMEOUT * 1000)
    val executor = new DefaultExecutor()
    val psh = new PumpStreamHandler(new LogOutputStream() {
      def processLine(line : String, lineNo : Int) { 
        line match {
          case tripleRegex(frag, prop, obj, _) => {
            context.addTriple(frag, prop, obj)
          }
          case _ => System.err.println("Ignoring STDOUT: " + line)
        }
      }
    }, new LogOutputStream() {
      def processLine(line : String, lineNo : Int) {
        System.err.println("STDERR: " + line)
      }
    })
    executor.setExitValue(1)
    executor.setWatchdog(watchdog)
    executor.setStreamHandler(psh)
    System.err.println("Start process: %s %s" format (command, args.mkString(" ")))
    executor.execute(cmdLine, handler)

    handler.waitFor()
  }
}
