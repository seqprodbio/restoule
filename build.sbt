name := """restoule"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "0.7.0",
  "org.postgresql" % "postgresql" % "9.2-1002-jdbc4",
  "org.apache.commons" % "commons-vfs2" % "2.0",
  "commons-net" % "commons-net" % "2.2",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "org.json" % "json" % "20140107",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  "org.apache.httpcomponents" % "httpmime" % "4.3.1",
  "io.spray" %%  "spray-json" % "1.2.6"
)
