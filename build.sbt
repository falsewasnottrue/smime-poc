name := """smime-poc"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "javax.mail" % "mail" % "1.4.7",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.60",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.60",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

