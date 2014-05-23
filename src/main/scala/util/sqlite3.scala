package ae.mccr.util

import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Simple utils for working with sqlite
 */
trait Connection {
  protected def connection : java.sql.Connection
  protected def statement : java.sql.Statement
  def prepare(query : String) : PrepQuery = new PrepQuery(connection.prepareStatement(query))
  def execute(query : String) : Boolean = statement.execute(query)
}

class PrepQuery(statement : PreparedStatement) {
  private trait rs2iter[X] extends Iterable[X] {
    def resultSet : ResultSet
    protected def _next : X
    def iterator = new Iterator[X] {
      val hn = resultSet.next
      def hasNext = hn
      def next = {
        val r = _next
        resultSet.next
        r
      }
    }
  }

  def int2int : Int => Iterable[Int] = {
    x : Int => {
      statement.setInt(1,x)
      val rs = statement.executeQuery
      new rs2iter[Int] {
        def resultSet = rs
        def _next = rs.getInt(1)
      }
    }
  }

  def int_string(p1 : Int, p2 : String) : Boolean = {
    statement.setInt(1, p1)
    statement.setString(2, p2)
    statement.execute
  }
}

private[util] class SQLiteConnection(file : String) extends Connection {
  try {
    Class.forName("org.sqlite.JDBC")
  } catch {
    case x : ClassNotFoundException => throw new RuntimeException("SQLite drive not available", x)
  }
  protected val connection = DriverManager.getConnection("jdbc:sqlite:" + file)
  protected val statement = connection.createStatement()

  def close {
    statement.close
    connection.close
  }
}

object sqlite3 {
  def apply[X](file : String)(foo : Connection => X) = {
    val conn = new SQLiteConnection(file)
    try {
      foo(conn)
    } finally {      
      conn.close
    }
  }
}
