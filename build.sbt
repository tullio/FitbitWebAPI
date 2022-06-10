val scala3Version = "3.1.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "fitbit",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % Test,
    libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5" % "5.1.2",
    libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5-fluent" % "5.1.2",
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.1",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1",
    //libraryDependencies += "io.circe" %% "circe-generic-extras" % "0.14.1",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1",
    libraryDependencies += "com.electronwill.night-config" %  "toml" % "3.6.0",
    libraryDependencies += "com.github.pathikrit" % "better-files_2.13" % "3.9.1",
    libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.5.1",
    libraryDependencies += "org.mapdb" % "mapdb" % "3.0.8",
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.30.0",
    libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
    libraryDependencies += "com.github.psambit9791" % "jdsp" % "1.0.0",
    libraryDependencies += "org.tinylog" % "tinylog" % "1.3.6",
    libraryDependencies += "org.tinylog" % "tinylog-api" % "2.1.2",
    libraryDependencies += "org.tinylog" % "tinylog-impl" % "2.1.2",
    libraryDependencies += "org.jetbrains.bio" % "npy" % "0.3.5"

  )
//        libraryDependencies += "com.github.sh0nk" % "matplotlib4j" % "0.5.0",

