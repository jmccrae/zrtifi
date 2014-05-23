package zrtifi

import org.scalatra._
import scalate.ScalateSupport
import ae.mccr.util._
import org.slf4j.{LoggerFactory, Logger}
import scala.math._
import java.util.concurrent.Executors

class ZrtifiServlet extends ZrtifiscratchStack {
  import ZrtifiProperties._ 
  val log = LoggerFactory.getLogger(getClass)

  val badgers = List(GZipBadger)
  val active = collection.mutable.Map[Int, List[Badging]]()
  
  override def initialize(config : ConfigT) = {
    super.initialize(config)
    sqlite3(DB_FILE) {
      connection =>
        if(!connection.execute("create table if not exists resources (id integer primary key autoincrement, resource_id integer unique, url text)")) {
          log.warn("Did not create table")
        }
    }
  }

  get("/") {
    params.get("url") match {
      case Some(url) => {
        sqlite3(DB_FILE) {
          connection => {
            val resourceId = abs(scala.util.Random.nextInt)
            connection.prepare("insert into resources (resource_id, url) values (?, ?)").int_string(resourceId, url) 
            val executor =  Executors.newSingleThreadExecutor()
            executor.submit(new Runnable {
              def run {
                val downloader = Downloader.download(url)
                active.put(resourceId, downloader :: Nil)
                downloader.run
                downloader.result match {
                  case Some(file) => {                    
                    var resource = file
                    var b = badgers.find(_.possible(resource))
                    while(b != None) {
                      val bg = b.get.start(resource)
                      active.put(resourceId, bg :: active(resourceId))
                      executor.submit(bg)
                      bg.result match {
                        case Some(f) => {
                          resource = f
                          b = badgers.find(_.possible(resource))
                        }
                        case None => b = None
                      }
                    }
                  }
                  case None => 
                }
              }
            })
            executor.shutdown
            TemporaryRedirect("/status/%d" format(resourceId))
          }
        }
      }
      case None => {
        BadRequest(reason="URL required")
      }
    }
  }

  get("/status/:id") {
    (for(badging <- active.getOrElse(params("id").toInt, Nil)) yield {
      badging.toString
    }).mkString("\n")    
  }
}
