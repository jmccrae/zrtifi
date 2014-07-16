package com.github.jmccrae.zrtifi

import java.io.File
import org.apache.commons.exec.{CommandLine, DefaultExecutor, DefaultExecuteResultHandler, ExecuteWatchdog, PumpStreamHandler}
import ZrtifiSettings._

class ProcessManager {
  private val processes = collection.mutable.Map[String,Thread]()
  private val folders = collection.mutable.Map[String,File]()
  private val statuses = collection.mutable.Map[String,String]()
  
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

  def startThread(runnable : ProcessRunnable) : String = {
    val id = math.abs(util.Random.nextInt).toString
    val context = new ProcessContextImpl(id)
    val thread = new Thread(new Runnable {
      def run {
        try {
          statuses += id -> ("Running %s" format runnable.getClass.getName)
          runnable.run(context)
          this.synchronized {
            wait(10000)
          }
          for(next <- context.chains) {
          statuses += id -> ("Running %s" format next.getClass.getName)
            next.run(context)
          }
        } finally {
          cleanUpProcess(id)
        }
      }
    })
    processes += id -> new Thread(thread)
    thread.start
    return id
  }

  private class ProcessContextImpl(id : String) extends ProcessContext {
    var chains : List[ProcessRunnable] = Nil
    def tempFolder = folderForProcess(id)
    def createTempFile(path : String) = ProcessManager.this.createTempFile(id, path)
    def chain(runnable : ProcessRunnable) { chains ::= runnable }
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
  def chains : Seq[ProcessRunnable]
}

class ExecRunnable(command : String, args : String*) extends ProcessRunnable {
  def run(context : ProcessContext) {
    val cmdLine = new CommandLine(command)
    for(arg <- args) {
      cmdLine.addArgument(arg)
    }
    val handler = new DefaultExecuteResultHandler()
    val ioHandler = new PumpStreamHandler()
    val watchdog = new ExecuteWatchdog(EXTERNAL_PROCESS_TIMEOUT * 1000)
    val executor = new DefaultExecutor()
    executor.setExitValue(1)
    executor.setWatchdog(watchdog)
    executor.setStreamHandler(ioHandler)
    executor.execute(cmdLine, handler)

    handler.waitFor()
  }
}
