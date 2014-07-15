name := """restoule"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.play" %% "play-slick" % "0.7.0-M1",
  "org.postgresql" % "postgresql" % "9.2-1002-jdbc4"
)
