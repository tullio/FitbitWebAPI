val scala3Version = "3.1.1"

fork := true

//import ai.kien.python.Python
/*
//lazy val python = Python("/usr/local/bin/python3")
lazy val python = Python("/usr/bin/python3.6m")

lazy val javaOpts = python.scalapyProperties.get.map {
  case (k, v) => s"""-D$k=$v"""
}.toSeq
javaOptions ++= javaOpts
 */
lazy val root = project
  .in(file("."))
  .settings(
    name := "fitbit",
    version := "0.1.0-SNAPSHOT",

//    fork in run := true,

    scalaVersion := scala3Version,
//    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    libraryDependencies +=
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",

    //libraryDependencies += "org.nd4j" % "nd4j-api" % "1.0.0-M2",
    //libraryDependencies += "org.nd4j" % "nd4j" % "1.0.0-M2",
    //libraryDependencies += "org.nd4j" % "nd4j-backend-impls" % "1.0.0-M1.1",
    //libraryDependencies += "org.nd4j" % "nd4j-native" % "1.0.0-M1.1",
    //libraryDependencies += "org.nd4j" % "nd4j-native-platform" % "1.0.0-M1.1",
    //libraryDependencies +=  "org.jfree" % "jfreechart" % "1.5.3",
//    libraryDependencies += "org.scalanlp" %% "breeze-viz" % "2.0.1-RC2",
//    libraryDependencies += "org.scalanlp" %% "breeze-viz" % "2.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % Test,
    libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5" % "5.1.3",
    libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5-fluent" % "5.1.3",
    libraryDependencies += "io.circe" %% "circe-core" % "0.15.0-M1",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1",
    //libraryDependencies += "io.circe" %% "circe-generic-extras" % "0.14.1",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1",
    libraryDependencies += "com.electronwill.night-config" %  "toml" % "3.6.5",
    //libraryDependencies += ("com.github.pathikrit" % "better-files_2.13" % "3.9.1").cross(CrossVersion.for3Use2_13),

    libraryDependencies += ("com.github.pathikrit" %% "better-files" % "3.9.1").cross(CrossVersion.for3Use2_13),
    libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.5.1",
    libraryDependencies += "org.mapdb" % "mapdb" % "3.0.8",
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.30.0",
    libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
    libraryDependencies += "com.github.psambit9791" % "jdsp" % "1.0.0",
    //libraryDependencies += "org.tinylog" % "tinylog" % "2.1.2",
    libraryDependencies += "org.tinylog" % "tinylog-api" % "2.5.0",
    libraryDependencies += "org.tinylog" % "tinylog-impl" % "2.5.0",
    libraryDependencies += "org.jetbrains.bio" % "npy" % "0.3.5",
    libraryDependencies += "me.shadaj" %% "scalapy-core" % "0.5.2",
    //libraryDependencies += "me.shadaj" %%% "scalapy-core" % "0.5.2",
    libraryDependencies += "ai.kien" %% "python-native-libs" % "0.2.2",
    libraryDependencies += "ai.djl" % "api" % "0.19.0",
    libraryDependencies += "ai.djl" % "basicdataset" % "0.19.0",
    libraryDependencies += "ai.djl" % "model-zoo" % "0.19.0",
//    libraryDependencies += "ai.djl.mxnet" % "mxnet-engine" % "0.19.0" % "runtime", 
    libraryDependencies += "ai.djl.mxnet" % "mxnet-engine" % "0.19.0" , 
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.32" , 

//    libraryDependencies += "ai.djl.mxnet" % "mxnet-native-mkl" % "1.9.1" % "runtime",
//    libraryDependencies += "net.java.dev.jna" % "jna" % "5.12.1",
//    libraryDependencies += "net.java.dev.jna" % "jna-platform" % "5.12.1"


  )
//        libraryDependencies += "com.github.sh0nk" % "matplotlib4j" % "0.5.0",


assemblyMergeStrategy in assembly := {
    case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
    case PathList("javax", "xml", xs @ _*)         => MergeStrategy.first
    case PathList("com", "fasterxml", xs @ _*)         => MergeStrategy.first
    case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("com", "google", xs @ _*) => MergeStrategy.last
    case PathList("META-INF", "services", xs @ _*) => MergeStrategy.last
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".json" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".so" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".a" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".dll" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "Any.tasty" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "FacadeCreator.tasty" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "epoll_x86_64.so" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "package.html" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".template" => MergeStrategy.first
    case "UnusedStubClass.class"  => MergeStrategy.first
    case "jetty-dir.css"                            => MergeStrategy.first
    case "application.conf"                            => MergeStrategy.concat
    case "unwanted.txt"                                => MergeStrategy.discard
    case PathList("module-info.class")                                => MergeStrategy.discard
    // Failed
    case "org.apache.hadoop.fs.FileSystem"                                => MergeStrategy.discard
    // Great!
    case PathList(p @ _*) if p.last == "org.apache.hadoop.fs.FileSystem" => MergeStrategy.discard
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}
assemblyMergeStrategy in assemblyPackageDependency := {
    case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
    case PathList("javax", "xml", xs @ _*)         => MergeStrategy.first
    case PathList("com", "fasterxml", xs @ _*)         => MergeStrategy.first
    case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("com", "google", xs @ _*) => MergeStrategy.last
    case PathList("org", "jfree", xs @ _*) => MergeStrategy.discard // obtain from submodule
    case PathList("META-INF", "services", xs @ _*) => MergeStrategy.last
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".json" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".so" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".a" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".dll" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "Any.tasty" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "FacadeCreator.tasty" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith "epoll_x86_64.so" => MergeStrategy.first
    case "UnusedStubClass.class"  => MergeStrategy.first
    case "jetty-dir.css"                            => MergeStrategy.first
    case "application.conf"                            => MergeStrategy.concat
    case "unwanted.txt"                                => MergeStrategy.discard
    case PathList("module-info.class")                                => MergeStrategy.discard
    // Failed
    case "org.apache.hadoop.fs.FileSystem"                                => MergeStrategy.discard
    // Great!
    case PathList(p @ _*) if p.last == "org.apache.hadoop.fs.FileSystem" => MergeStrategy.discard
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}
