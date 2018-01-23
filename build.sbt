name := "My Project"
 
version := "1.0"
 
scalaVersion := "2.12.4"
 
resolvers ++= Seq(
  "Geotools" at "http://download.osgeo.org/webdav/geotools",
  "Geotoolkit" at "http://maven.geotoolkit.org/",
  "JBoss" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases",
  "MATSim" at "http://dl.bintray.com/matsim/matsim",
  "Jitpack" at "https://jitpack.io",
  "Conveyal" at "http://maven.conveyal.com/",
  "java.net" at "http://download.java.net/maven/2/",
  "axis2" at "http://people.apache.org/repo/m1-ibiblio-rsync-repository/org.apache.axis2/",
  "Graphql" at "http://dl.bintray.com/andimarek/graphql-java",
  "Geosolutions" at "http://maven.geo-solutions.it",
  "onebusaway" at "http://nexus.onebusaway.org/content/groups/public/",
  "axiomalaska" at "http://nexus.axiomalaska.com/nexus/content/repositories/public/"
)

updateOptions := updateOptions.value.withGigahorse(false)
updateConfiguration in updateSbtClassifiers := (updateConfiguration in updateSbtClassifiers).value.withMissingOk(true)

 
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.16" % "test",
  "org.matsim" % "matsim" % "0.10.0-beam-1" exclude("log4j","log4j"),
  "org.slf4j" % "log4j-over-slf4j" % "1.7.12",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "net.sf.supercsv" % "super-csv" % "2.4.0",
  "com.github.michaz" % "r5" % "master-SNAPSHOT" exclude("com.axiomalaska","polyline-encoder") exclude("ch.qos.logback", "logback-classic") exclude("org.slf4j", "slf4j-simple"),
  "com.axiomalaska" % "polyline-encoder" % "0.1-SNAPSHOT",
  "com.beachape" %% "enumeratum" % "1.5.12",
  "com.beachape" %% "enumeratum-circe" % "1.5.12",
  "com.hubspot.jinjava" % "jinjava" % "2.0.5",
  "org.yaml" % "snakeyaml" % "1.18",
  "io.kamon" %% "kamon-core" % "0.6.7",
  "io.kamon" %% "kamon-log-reporter" % "0.6.7",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "tv.cntt" %% "glokka" % "2.4.0",
  "org.reflections" % "reflections" % "0.9.10",
  "com.google.inject.extensions" % "guice-assistedinject" % "4.1.0",
  "com.typesafe.play" %% "play-json" % "2.6.3",
  "org.apache.logging.log4j" % "log4j-api" % "2.9.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0",
  "org.apache.httpcomponents" % "fluent-hc" % "4.5.2",
  "com.google.code.gson" % "gson" % "2.8.2",
  "javax.annotation" % "javax.annotation-api" % "1.2",
  "junit" % "junit" % "4.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

excludeDependencies += "javax.media" % "jai_core"

fork := true
