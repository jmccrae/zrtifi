package com.github.jmccrae.zrtifi

object ZrtifiSettings {
  // This file contains all relevant configuration for the system

  // The location where this server is to be deployed to
  // Only URIs in the dump that start with this address will be published
  // Should end with a trailing /
  val BASE_NAME = "http://localhost:8080/"
  // Where the SQLite database should appear
  val DB_FILE = "db.sqlite"
  // The name of the server
  val DISPLAY_NAME = "Because Data Quality Matters"
  // The dump URI
  val DUMP_URI = "/zrtifi.nt.gz"

  // The extra namespaces to be abbreviated in HTML and RDF/XML documents if desired
  val PREFIX1_QN = "zrtifi"
  val PREFIX2_URI = "http://www.w3.org/ns/dcat#"
  val PREFIX2_QN = "dcat"
  val PREFIX3_URI = "http://www.example.com#"
  val PREFIX3_QN = "example"
  val PREFIX4_URI = "http://rdfs.org/ns/void#"
  val PREFIX4_QN = "void"
  val PREFIX5_URI = "http://www.example.com#"
  val PREFIX5_QN = "ex5"
  val PREFIX6_URI = "http://www.example.com#"
  val PREFIX6_QN = "ex6"
  val PREFIX7_URI = "http://www.example.com#"
  val PREFIX7_QN = "ex7"
  val PREFIX8_URI = "http://www.example.com#"
  val PREFIX8_QN = "ex8"
  val PREFIX9_URI = "http://www.example.com#"
  val PREFIX9_QN = "ex9"

  // If using an external SPARQL endpoint, the address of this
  // or None if you wish to use built-in (very slow) endpoint
  val SPARQL_ENDPOINT : Option[String] = None
  // Path to the license (set to null to disable)
  val LICENSE_PATH = "/license.html"
  // Path to the search (set to null to disable)
  val SEARCH_PATH = "/search"
  // Path to static assets
  val ASSETS_PATH = "/assets/"
  // Path to SPARQL (set to null to disable)
  val SPARQL_PATH = "/sparql"
  // Path to site contents list (set to null to disable)
  val LIST_PATH = "/list"
  // Path to the process manager status
  val PROCESS_STATUS_PATH = "/process_status"

  // Amount of time (seconds) to wait for external process
  val EXTERNAL_PROCESS_TIMEOUT = 7200
  // The location of the Zrtifi internal URIs
  val ZRTIFI_INTERNAL = "http://www.zrtifi.org/internal#"
  // The Zrtifi ontology
  val ZRTIFI_ONTOLOGY = "http://www.zrtifi.org/ontology#"
  val PREFIX1_URI = ZRTIFI_ONTOLOGY

}
