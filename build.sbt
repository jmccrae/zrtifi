name := "zrtifi"

version := "0.1"

scalaVersion := "2.10.3"

seq(webSettings :_*)

resolvers += "Monnet Repository" at "http://monnet01.sindice.net/mvn/"

libraryDependencies ++= Seq(
  "org.apache.jena" % "jena-arq" % "2.11.1",
  "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "org.scalatest" %% "scalatest" % "2.1.7" % "test",
  "gnu.getopt" % "java-getopt" % "1.0.13",
  "org.mockito" % "mockito-all" % "1.9.0",
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "container",
  "org.eclipse.jetty" % "jetty-plus"   % "9.1.0.v20131115" % "container",
  "eu.monnetproject" % "framework.services" % "1.13.3" exclude("org.apache.felix", "javax.servlet"),
  "org.apache.commons" % "commons-exec" % "1.2"
)


scalacOptions := Seq("-feature", "-deprecation")
