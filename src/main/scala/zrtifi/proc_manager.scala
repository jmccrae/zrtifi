package com.github.jmccrae.zrtifi

import java.io.File
import scala.collection.immutable.Queue
import org.apache.commons.exec.{CommandLine, DefaultExecutor, DefaultExecuteResultHandler, ExecuteException, ExecuteWatchdog, PumpStreamHandler, LogOutputStream}
import ZrtifiSettings._
import java.util.UUID

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
    context.addTriple("","<%sprocessing>" format ZRTIFI_ONTOLOGY,"\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>")
    val thread = new Thread(new Runnable {
      def run {
        try {
          statuses += id -> ("Running %s" format runnable.getClass.getName)
          System.err.println("Running %s" format runnable.getClass.getName)
          runnable.run(context)
          var next = context.next
          while(next != None) {
            statuses += id -> ("Running %s" format next.get.getClass.getName)
            System.err.println("Running %s" format next.get.getClass.getName)
            next.get.run(context)
            println("finished running")
            next = context.next
          }
       } finally {
          context.removeTriples(Some(""),Some("<%sprocessing>" format ZRTIFI_ONTOLOGY))
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
        case Some(rdfID) => if(obj.startsWith("<#")) {
          dataModel.insertTriple(rdfID, frag, prop, "<%s%s#%s>" format (BASE_NAME, rdfID, obj.slice(2,obj.size - 1)))
        } else {
          dataModel.insertTriple(rdfID, frag, prop, obj)
        }
        case None => System.err.println("No rdfID for this process! (Should not happen)")
      }
    }
    def removeTriples(frag : Option[String] = None, prop : Option[String] = None) {
      rdfIDs.get(id) match {
        case Some(rdfID) => dataModel.removeTriples(rdfID, frag, prop)
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

class ExecChainer {
  var nextTarget : Option[File] = None
  var nextExec : Option[String] = None
  
  def chain : Option[ProcessRunnable] = nextTarget match {
    case Some(t) => nextExec match {
      case Some("sniff") => { println("sniffing " + t) ; Some(new SnifferRunnable(t)) }
      case Some(e) => { println("execute " + e) ; ExecRunnable.resolveExecutable(e, t) }
      case None => { println( "No exec") ; None }
    }
      case None => { println("No target") ; None }
  }
}


object ExecRunnable {
  def resolveExecutable(name : String, file : File) : Option[ProcessRunnable] = {
    val f = new File(new File("analyzers"), name)
    if(f.exists()) {
      return Some(new ExecRunnable(f.getAbsolutePath(), file.getAbsolutePath()))
    }
    val f_py = new File(new File("analyzers"), name + ".py")
    if(f_py.exists()) {
      return Some(new ExecRunnable("python", f_py.getAbsolutePath(), file.getAbsolutePath()))
    }
    return None
  }
}

class ExecRunnable(command : String, args : String*) extends ProcessRunnable {
  val tripleRegex = "<(#([^>]*))?>\\s+(<[^>]+>)\\s+(\".*?(?<!\\\\)\"(|@[\\w-]+|\\^\\^<[^>]+>)|<[^>]+>)\\s*\\.\\s*".r
  val zrtifiInternal = ("<%s(.*)>" format java.util.regex.Pattern.quote(ZRTIFI_INTERNAL)).r

  def run(context : ProcessContext) {
    val mutex = new Object()
    val cmdLine = new CommandLine(command)
    for(arg <- args) {
      cmdLine.addArgument(arg)
    }
    val chainers = collection.mutable.Map[String,ExecChainer]()
    def getChainer(id : String) = chainers.getOrElse(id, {
      val newChain = new ExecChainer()
      chainers.put(id, newChain)
      newChain
    })
    val handler = new DefaultExecuteResultHandler() {
      override def onProcessComplete(exitValue : Int) {
        System.err.println("Finish process (status=%d): %s %s" format (exitValue, command, args.mkString(" ")))
        for(chain <- chainers.values) {
          chain.chain match {
            case Some(chain) => {
              println("chaining")
              context.chain(chain)
            }
            case None => // stop
          }
        }
        mutex.synchronized {
          mutex.notify()
        }
      }
      override def onProcessFailed(e : ExecuteException) {
        e.printStackTrace()
      }
    } 
    val watchdog = new ExecuteWatchdog(EXTERNAL_PROCESS_TIMEOUT * 1000)
    val executor = new DefaultExecutor()
    val psh = new PumpStreamHandler(new LogOutputStream() {
      def processLine(line : String, lineNo : Int) { 
        line match {
          case tripleRegex(_, frag, prop, obj, _) => {
            println("%s %s %s" format (frag, prop, obj))
            prop match {
              case zrtifiInternal(prop) => prop match {
                case "next" => getChainer(frag).nextExec = Some(obj.slice(1,obj.size-1))
                case "nextTarget" => getChainer(frag).nextTarget = Some(new File(obj.slice(1,obj.size-1)))
                case _ => println("internal")// ignore internal URI
              }
              case _ => context.addTriple(Option(frag).getOrElse(""), prop, obj)
            }
          }
          case _ => System.err.println("Ignoring STDOUT: " + line)
        }
      }
    }, new LogOutputStream() {
      def processLine(line : String, lineNo : Int) {
        System.err.println("STDERR: " + line)
      }
    })
    executor.setExitValue(0)
    executor.setWatchdog(watchdog)
    executor.setStreamHandler(psh)
    System.err.println("Start process: %s %s" format (command, args.mkString(" ")))
    try {
      executor.execute(cmdLine, handler)
    } catch {
      case x : org.apache.commons.exec.ExecuteException => {
        val stepID = "step_" + UUID.randomUUID().toString()
        context.addTriple("","<%step>" format ZRTIFI_ONTOLOGY, "<#%s>" format (stepID))
        context.addTriple(stepID,"<%sstatus>" format ZRTIFI_ONTOLOGY, "<%serror>" format ZRTIFI_ONTOLOGY)
        context.addTriple(stepID,"<%serror>" format ZRTIFI_ONTOLOGY, "\"%s\"" format x.getMessage())
        
      }
    }

    //handler.waitFor()
    mutex.synchronized {
      try {
        mutex.wait()
      } catch {
        case x : InterruptedException =>
      }
    }
    System.err.println("handler finished waiting")
  }
}
