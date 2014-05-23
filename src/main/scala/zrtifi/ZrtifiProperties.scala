package zrtifi

import org.slf4j.{Logger, LoggerFactory}

object ZrtifiProperties {
  private val logger = LoggerFactory.getLogger(getClass)

  private val properties = new java.util.Properties
  try {
    properties.load(this.getClass().getResourceAsStream("/WEB-INF/settings.ini"))
  } catch {
    case x : Exception => logger.warn("Could not read settings.ini")
  }

  val DB_FILE = Option(properties.getProperty("DB_FILE")).getOrElse("test.db")
}
