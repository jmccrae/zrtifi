package com.github.jmccrae.zrtifi

import com.github.jmccrae.zrtifi.ZrtifiSettings._
import com.github.jmccrae.zrtifi.ZrtifiUserText._
import com.hp.hpl.jena.graph.{NodeFactory, Triple, TripleMatch, Node}
import com.hp.hpl.jena.util.iterator.ExtendedIterator
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory, AnonId, Resource}
import java.sql.{DriverManager, ResultSet}
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import gnu.getopt.Getopt

class RDFBackend(db : String) {
  try {
    Class.forName("org.sqlite.JDBC")
  } catch {
    case x : ClassNotFoundException => throw new RuntimeException("No SQLite Driver", x)
  }

  private val conn = DriverManager.getConnection("jdbc:sqlite:" + db)

  def close() {
    if(!conn.isClosed()) {
      conn.close()
    }
  }

  def graph = new RDFBackendGraph(conn)
 
  def from_n3(n3 : String, model : Model) = if(n3.startsWith("<") && n3.endsWith(">")) {
    model.createResource(n3.drop(1).dropRight(1))
  } else if(n3.startsWith("_:")) {
    model.createResource(AnonId.create(n3.drop(2)))
  } else if(n3.startsWith("\"") && n3.contains("^^")) {
    val Array(lit,typ) = n3.split("\"\\^\\^",2)
    model.createTypedLiteral(lit.drop(1), NodeFactory.getType(typ.drop(1).dropRight(1)))
  } else if(n3.startsWith("\"") && n3.contains("\"@")) {
    val Array(lit,lang) = n3.split("\"@", 2)
    model.createLiteral(lit.drop(1), lang)
  } else if(n3.startsWith("\"") && n3.endsWith("\"")) {
    model.createLiteral(n3.drop(1).dropRight(1))
  } else {
    throw new IllegalArgumentException("Not N3: %s" format n3)
  }

  def make_prop(uri : String, model : Model) = if(uri.contains('#')) {
    val (prop,frag) = uri.splitAt(uri.indexOf('#')+1)
    model.createProperty(prop,frag)
  } else if(uri.contains("/")) {
    val (prop,frag) = uri.splitAt(uri.lastIndexOf('/'))
    model.createProperty(prop,frag)
  } else {
    model.createProperty(uri,"")
  }

  def prop_from_n3(n3 : String, model : Model) =  if(n3.startsWith("<") && n3.endsWith(">")) {
    make_prop(n3.drop(1).dropRight(1), model)
  } else {
    throw new IllegalArgumentException("Not N3: %s" format n3)
  }

  def lookup(id : String) : Option[Model] = {
    val ps = conn.prepareStatement("select fragment, property, object, inverse from triples where subject=?")
    ps.setString(1,id)
    val rows = ps.executeQuery()
    if(!rows.next()) {
      return None
    }
    val model = ModelFactory.createDefaultModel()
    do {
      val f = rows.getString(1)
      val p = rows.getString(2)
      val o = rows.getString(3)
      val i = rows.getInt(4)
      if(i != 0) {
        val subject = from_n3(o, model)
        val property = prop_from_n3(p, model)
        val obj = model.createResource(RDFBackend.name(id, Option(f)))
        subject match {
          case r : Resource => r.addProperty(property, obj)
        }
      } else {
        val subject = model.createResource(RDFBackend.name(id, Option(f)))
        val property = prop_from_n3(p, model)
        val obj = from_n3(o, model)
        subject match {
          case r : Resource => r.addProperty(property, obj)
        }
        if(o.startsWith("_:")) {
          lookupBlanks(model, obj.asInstanceOf[Resource])
        }
      }
    } while(rows.next())
    return Some(model)
  }

  def lookupBlanks(model : Model, bn : Resource) {
    val ps = conn.prepareStatement("select property, object from triples where subject=\"<BLANK>\" and fragment=?")
    ps.setString(1, bn.getId().getLabelString())
    val rows = ps.executeQuery()
    while(rows.next()) {
      val p = rows.getString(1)
      val o = rows.getString(2)
      val property = prop_from_n3(p, model)
      val obj = from_n3(o, model)
      bn.addProperty(property, obj)
      if(o.startsWith("_:")) {
        lookupBlanks(model, obj.asInstanceOf[Resource])
      }
    }
  }

  private def litFromN3(lit : String) = lit.slice(1,lit.lastIndexOf("\""))

  def search(query : String, property : Option[String], limit : Int = 20) : List[(String, String)] = {
    val ps = property match {
      case Some(p) => {
        val ps2 = conn.prepareStatement("select distinct subject from triples where property=? and object like ? limit ?")
        ps2.setString(1, "<%s>" format p)
        ps2.setString(2, "%%%s%%" format query)
        ps2.setInt(3, limit)
        ps2
      } 
      case None => {
        val ps2 = conn.prepareStatement("select distinct subject from triples where object like ? limit ?")
        ps2.setString(1, "%%%s%%" format query)
        ps2.setInt(2, limit)
        ps2
      }
    }
    val results = collection.mutable.ListBuffer[(String, String)]()
    val rs = ps.executeQuery()
    while(rs.next()) {
      val ps3 = conn.prepareStatement("select object from triples where subject=? and property='<http://www.w3.org/2000/01/rdf-schema#label>'")
      ps3.setString(1, rs.getString(1))
      val rs2 = ps3.executeQuery()    
      results += ((rs.getString(1), litFromN3(rs2.getString(1))))
    }
    return results.toList
  }

  def listResources(offset : Int, limit : Int) : (Boolean,List[(String, String)]) = {
    val ps = conn.prepareStatement("select distinct subject, object from triples where property='<http://www.w3.org/2000/01/rdf-schema#label>' limit ? offset ?")
    ps.setInt(1, limit + 1)
    ps.setInt(2, offset)
    val rs = ps.executeQuery()
    var n = 0
    if(!rs.next()) {
      return (false, Nil)
    }
    var results = collection.mutable.ListBuffer[(String, String)]()
    do {
      results += ((rs.getString(1), litFromN3(rs.getString(2))))
      n += 1
    } while(rs.next())

    if(n >= limit) {
      results.remove(n - 1)
      return (true, results.toList)
    } else {
      return (false, results.toList)
    }
  }

  def insertTriple(id : String, frag : String, prop : String, obj : String) {
    try {
    val ps1 = conn.prepareStatement("insert into triples values (?, ?, ?, ?, 0)")
    ps1.setString(1, RDFBackend.unicodeEscape(id))
    ps1.setString(2, RDFBackend.unicodeEscape(frag))
    ps1.setString(3, RDFBackend.unicodeEscape(prop))
    ps1.setString(4, RDFBackend.unicodeEscape(obj))
    ps1.execute()
    } catch {
      case x : Exception => {
        println("Failed to add triple %s#%s %s %s" format (id, frag, prop, obj))
        throw x
      }
    }
  }

  def removeTriples(id : String, frag : Option[String] = None, prop : Option[String] = None) {
    frag match {
      case Some(f) => prop match {
        case Some(p) => {
          val ps1 = conn.prepareStatement("delete from triples where subject=? and fragment=? and property=?")
          ps1.setString(1, id)
          ps1.setString(2, f)
          ps1.setString(3, p)
          ps1.execute()
        }
        case None => {
          val ps1 = conn.prepareStatement("delete from triples where subject=? and fragment=?")
          ps1.setString(1, id)
          ps1.setString(2, f)
          ps1.execute()
        }
      }
      case None => prop match {
        case Some(p) => {
          val ps1 = conn.prepareStatement("delete from triples where subject=? and property=?")
          ps1.setString(1, id)
          ps1.setString(2, p)
          ps1.execute()
        }
        case None => {
          val ps1 = conn.prepareStatement("delete from triples where subject=?")
          ps1.setString(1, id)
          ps1.execute()
        }
      }
    }
  }

  private var isInitialized = false
  def init() {
    if(!isInitialized) {
      val cursor = conn.createStatement()
      val oldAutocommit = conn.getAutoCommit()
      try {
        conn.setAutoCommit(false)
        cursor.execute("create table if not exists [triples] ([subject] TEXT, [fragment] TEXT, property TEXT NOT NULL, object TEXT NOT NULL, inverse INT DEFAULT 0)")
        cursor.execute("create index if not exists k_triples_subject ON [triples] ( subject )")
      } finally {
        cursor.close()
        conn.commit()
        conn.setAutoCommit(oldAutocommit)
      }
      isInitialized = true
    }
  }
}

class RDFBackendGraph(conn : java.sql.Connection)  extends com.hp.hpl.jena.graph.impl.GraphBase {
  protected def graphBaseFind(m : TripleMatch) : ExtendedIterator[Triple] = {
    val model = ModelFactory.createDefaultModel()
    val s = m.getMatchSubject()
    val p = m.getMatchPredicate()
    val o = m.getMatchObject()
    val rs : ResultSet = if(s == null) {
      if(p == null) {
        if(o == null) {
          throw new RuntimeException(YZ_QUERY_TOO_BROAD)
        } else {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where object=? and inverse=0")
          ps.setString(1, RDFBackend.to_n3(o))
          ps.executeQuery()        
        }
      } else {
        if(o == null) {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where property=? and inverse=0")
          ps.setString(1, RDFBackend.to_n3(p))
          ps.executeQuery()
        } else {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where property=? and inverse=0 and object=?")
          ps.setString(1, RDFBackend.to_n3(p))
          ps.setString(2, RDFBackend.to_n3(o))
          ps.executeQuery()
        }
      }
    } else {
      val (id, frag) = RDFBackend.unname(s.toString()) match {
        case Some((i,f)) => (i,f)
        case None => return new NullExtendedIterator()
      }
      if(p == null) {
        if(o == null) {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where subject=? and fragment=? and inverse=0")
          ps.setString(1, id)
          ps.setString(2, frag.getOrElse(""))
          ps.executeQuery()
        } else {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where subject=? and fragment=? and object=? and inverse=0")
          ps.setString(1, id)
          ps.setString(2, frag.getOrElse(""))
          ps.setString(3, RDFBackend.to_n3(o))
          ps.executeQuery()
        }
      } else {
        if(o == null) {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where subject=? and fragment=? and property=? and inverse=0")
          ps.setString(1, id)
          ps.setString(2, frag.getOrElse(""))
          ps.setString(3, RDFBackend.to_n3(p))
          ps.executeQuery()
        } else {
          val ps = conn.prepareStatement("select subject, fragment, property, object from triples where subject=? and fragment=? and property=? and object=? and inverse=0")
          ps.setString(1, id)
          ps.setString(2, frag.getOrElse(""))
          ps.setString(3, RDFBackend.to_n3(p))
          ps.setString(4, RDFBackend.to_n3(o))
          ps.executeQuery()
        }
      }
    }
    return new SQLResultSetAsExtendedIterator(rs)
  }
}

class NullExtendedIterator() extends ExtendedIterator[Triple] {
  def close() { }
  def andThen[X <: Triple](x : java.util.Iterator[X]) : ExtendedIterator[Triple] = throw new UnsupportedOperationException()
  def filterDrop(x : com.hp.hpl.jena.util.iterator.Filter[com.hp.hpl.jena.graph.Triple]):
  com.hp.hpl.jena.util.iterator.ExtendedIterator[com.hp.hpl.jena.graph.Triple] = throw new UnsupportedOperationException()
  def filterKeep(x : com.hp.hpl.jena.util.iterator.Filter[com.hp.hpl.jena.graph.Triple]):
  com.hp.hpl.jena.util.iterator.ExtendedIterator[com.hp.hpl.jena.graph.Triple] = throw new UnsupportedOperationException()
  def mapWith[U](x: com.hp.hpl.jena.util.iterator.Map1[com.hp.hpl.jena.graph.Triple,U]): com.hp.hpl.jena.util.iterator.ExtendedIterator[U] =
    throw new UnsupportedOperationException()
  def removeNext(): com.hp.hpl.jena.graph.Triple =  throw new UnsupportedOperationException()
  def toList(): java.util.List[com.hp.hpl.jena.graph.Triple] =  throw new UnsupportedOperationException()
  def toSet(): java.util.Set[com.hp.hpl.jena.graph.Triple] = throw new UnsupportedOperationException()

  def hasNext(): Boolean = false
  def next(): Triple = throw new NoSuchElementException()
  def remove(): Unit = throw new UnsupportedOperationException()
}

class SQLResultSetAsExtendedIterator(rs : ResultSet) extends ExtendedIterator[Triple] {
  def close() { rs.close() }
  def andThen[X <: Triple](x : java.util.Iterator[X]) : ExtendedIterator[Triple] = throw new UnsupportedOperationException()
  def filterDrop(x : com.hp.hpl.jena.util.iterator.Filter[com.hp.hpl.jena.graph.Triple]):
  com.hp.hpl.jena.util.iterator.ExtendedIterator[com.hp.hpl.jena.graph.Triple] = throw new UnsupportedOperationException()
  def filterKeep(x : com.hp.hpl.jena.util.iterator.Filter[com.hp.hpl.jena.graph.Triple]):
  com.hp.hpl.jena.util.iterator.ExtendedIterator[com.hp.hpl.jena.graph.Triple] = throw new UnsupportedOperationException()
  def mapWith[U](x: com.hp.hpl.jena.util.iterator.Map1[com.hp.hpl.jena.graph.Triple,U]): com.hp.hpl.jena.util.iterator.ExtendedIterator[U] =
    throw new UnsupportedOperationException()
  def removeNext(): com.hp.hpl.jena.graph.Triple =  throw new UnsupportedOperationException()
  def toList(): java.util.List[com.hp.hpl.jena.graph.Triple] =  throw new UnsupportedOperationException()
  def toSet(): java.util.Set[com.hp.hpl.jena.graph.Triple] = throw new UnsupportedOperationException()

  def from_n3(n3 : String) = if(n3.startsWith("<") && n3.endsWith(">")) {
    NodeFactory.createURI(n3.drop(1).dropRight(1))
  } else if(n3.startsWith("_:")) {
    NodeFactory.createAnon(AnonId.create(n3.drop(2)))
  } else if(n3.startsWith("\"") && n3.contains("^^")) {
    val Array(lit,typ) = n3.split("\"\\^\\^",2)
    NodeFactory.createLiteral(lit.drop(1), NodeFactory.getType(typ.drop(1).dropRight(1)))
  } else if(n3.startsWith("\"") && n3.contains("\"@")) {
    val Array(lit,lang) = n3.split("\"@", 2)
    NodeFactory.createLiteral(lit.drop(1), lang, false)
  } else if(n3.startsWith("\"") && n3.endsWith("\"")) {
    NodeFactory.createLiteral(n3.drop(1).dropRight(1))
  } else {
    throw new IllegalArgumentException("Not N3: %s" format n3)
  }

  def make_prop(uri : String) = NodeFactory.createURI(uri)

  def prop_from_n3(n3 : String) =  if(n3.startsWith("<") && n3.endsWith(">")) {
    make_prop(n3.drop(1).dropRight(1))
  } else {
    throw new IllegalArgumentException("Not N3: %s" format n3)
  }


  private var _hasNext = rs.next()
  def hasNext(): Boolean = _hasNext
  def next(): Triple = {
    val s = rs.getString("subject")
    val f = rs.getString("fragment")
    val p = rs.getString("property")
    val o = rs.getString("object")
    val t = new Triple(NodeFactory.createURI(RDFBackend.name(s, Option(f))),
      prop_from_n3(p),
      from_n3(o))
    _hasNext = rs.next()

    return t
  }
  def remove(): Unit = throw new UnsupportedOperationException()
}

object RDFBackend {

  def name(id : String, frag : Option[String]) = frag match {
    case Some("") => "%s%s" format (BASE_NAME, id)
    case Some(f) => "%s%s#%s" format (BASE_NAME, id, f)
    case None => "%s%s" format (BASE_NAME, id)
  }

  def unname(uri : String) = if(uri.startsWith(BASE_NAME)) {
    if(uri contains '#') {
      val id = uri.slice(BASE_NAME.length, uri.indexOf('#'))
      val frag = uri.drop(uri.indexOf('#') + 1)
      Some((id, Some(frag)))
    } else {
      Some((uri.drop(BASE_NAME.length), None))
    }
  } else {
    None
  }


  def to_n3(node : Node) : String = if(node.isURI()) {
    return "<%s>" format node.getURI()
  } else if(node.isBlank()) {
    return "_:%s" format node.getBlankNodeId().toString()
  } else if(node.getLiteralLanguage() != "") {
    return "\"%s\"@%s" format (node.getLiteralValue().toString().replaceAll("\"","\\\\\""), node.getLiteralLanguage())
  } else if(node.getLiteralDatatypeURI() != null) {
    return "\"%s\"^^<%s>" format (node.getLiteralValue().toString().replaceAll("\"","\\\\\""), node.getLiteralDatatypeURI())
  } else {
    return "\"%s\"" format (node.getLiteralValue().toString().replaceAll("\"","\\\\\""))
  }

  def unicodeEscape(str : String) : String = {
    val sb = new StringBuilder(str)
    var i = 0
    while(i < sb.length) {
      if(sb.slice(i,i+2).toString == "\\u") {
        sb.replace(i,i+6, Integer.parseInt(sb.slice(i+2,i+6).toString, 16).toChar.toString)
      }
      i += 1
    }
    return sb.toString
  }

  def main(args : Array[String]) {
    val getopt = new Getopt("zrtifibackend", args, "f:")
    var opts = collection.mutable.Map[String, String]()
    var c = 0
    while({c = getopt.getopt(); c } != -1) {
      c match {
        case 'd' => opts("-d") = getopt.getOptarg()
        case 'f' => opts("-f") = getopt.getOptarg()
      }
    }
    val backend = new RDFBackend(opts.getOrElse("-d", DB_FILE))
    backend.init()
    backend.close()
  }
}
