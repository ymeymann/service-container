import sbt.Keys._
import sbt._

object ContainerBuild extends Build {

  val AKKA_VERSION = "2.3.11"
  val SPRAY_VERSION = "1.3.3"
  val CONFIG_VERSION = "1.3.0"
  val METRICS_VERSION = "3.1.2"
  val JSON4S_VERSION = "3.2.11"
  val SPECS_VERSION = "3.6.1"
  val JDK = "1.8"

  lazy val baseSettings = Seq(
    name := "Service Container",
    organization := "com.github.vonnagy",
    version := "1.0.3-SNAPSHOT",
    description := "Service Container",
    scalaVersion := "2.11.6",
    crossScalaVersions := Seq("2.10.5", "2.11.6")
  )

  override val settings = super.settings ++ baseSettings

  lazy val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

  lazy val defaultSettings = Test.testSettings ++ Seq(

    logLevel := Level.Info,

    scalacOptions ++= Seq("-encoding", "UTF-8", "-feature", "-language:_", "-deprecation", "-unchecked"),
    javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", JDK, "-target", JDK,
      "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:-options"),

    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)),
    runMain in Compile <<= Defaults.runMainTask(fullClasspath in Compile, runner in (Compile, run)),

    resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",

    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },

    parallelExecution in ThisBuild := false,
    parallelExecution in Global := false
  )

  object Dependencies {

    object Compile {
      val sprayCan        = "io.spray"              %%  "spray-can"         % SPRAY_VERSION
      val sprayHttp       = "io.spray"              %%  "spray-http"        % SPRAY_VERSION
      val sprayHttpx      = "io.spray"              %%  "spray-httpx"       % SPRAY_VERSION
      val sprayRouting    = "io.spray"              %%  "spray-routing"     % SPRAY_VERSION
      val sprayJson       = "io.spray"              %%  "spray-json"        % "1.3.2"
      val config          = "com.typesafe"          %   "config"            % CONFIG_VERSION
      val akkaActor       = "com.typesafe.akka"     %%  "akka-actor"        % AKKA_VERSION
      val akkaSlf4j       = "com.typesafe.akka"     %%  "akka-slf4j"        % AKKA_VERSION
      val akkaRemote      = "com.typesafe.akka"     %%  "akka-remote"       % AKKA_VERSION
      val slf4j           = "org.slf4j"             %   "slf4j-api"         % "1.7.12"
      val logback         = "ch.qos.logback"        %   "logback-classic"   % "1.1.3"
      val slf4jOverLog4j  = "org.slf4j"             %   "log4j-over-slf4j"  % "1.7.12"
      val json4sCore      = "org.json4s"            %%  "json4s-core"       % JSON4S_VERSION
      val json4sJackson   = "org.json4s"            %%  "json4s-jackson"    % JSON4S_VERSION
      val json4sExt       = "org.json4s"            %%  "json4s-ext"        % JSON4S_VERSION
      val metricsCore     = "io.dropwizard.metrics" %   "metrics-core"      % METRICS_VERSION
      val metricsJvm      = "io.dropwizard.metrics" %   "metrics-jvm"       % METRICS_VERSION
      val joda            = "joda-time"             %   "joda-time"         % "2.8.1"

      val akkaKafka       = "com.sclasen"           %%  "akka-kafka"        % "0.1.0"
      val metricsStatsd   = "com.github.jjagged"    %   "metrics-statsd"    % "1.0.0" exclude("com.codahale.metrics", "metrics")
      val metricsInflux   = "com.novaquark"         %   "metrics-influxdb"  % "0.3.0" excludeAll(ExclusionRule("com.codahale.metrics"))
      val metricsDataDog  = "org.coursera"          %   "metrics-datadog"   % "1.1.1"
    }
    
    object Test {
      val akkaTest        = "com.typesafe.akka"     %%  "akka-testkit"      % AKKA_VERSION  % "test"
      val sprayTest       = "io.spray"              %%  "spray-testkit"     % SPRAY_VERSION % "test"
      val specsCore       = "org.specs2"            %%  "specs2-core"       % SPECS_VERSION % "test"
      val specsMock       = "org.specs2"            %%  "specs2-mock"       % SPECS_VERSION % "test"
      val scalazStream    = "org.scalaz.stream"     %%  "scalaz-stream"     % "0.7a"        % "test"
    }

    import Dependencies.Compile._
    import Dependencies.Test._

    val akka = Seq(akkaActor, akkaSlf4j, akkaRemote)
    val spray = Seq(sprayCan, sprayHttp, sprayHttpx, sprayRouting, sprayJson)
    val json4s = Seq(json4sCore, json4sJackson, json4sExt)
    val logging = Seq(logback, slf4j, slf4jOverLog4j)
    val metrics = Seq(metricsCore, metricsJvm)

    val base = akka ++ spray ++ json4s ++ logging ++ metrics ++ Seq(joda)
    val test = Seq(akkaTest, sprayTest, specsCore, specsMock, scalazStream)

    val core = base ++ test
    val reporting = test ++ Seq(metricsStatsd, metricsInflux, metricsDataDog)
    val examples = Seq(akkaKafka)

    val overrrides = Set(metricsCore, slf4j)
  }

  lazy val moduleSettings = defaultSettings ++ Publish.settings

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = noPublishSettings ++ baseSettings,
    aggregate = Seq(container, metricsReporting, examples)
  )

  lazy val container = Project(
    id = "service-container",
    base = file("./service-container"),
    settings = moduleSettings
        ++ Seq(libraryDependencies ++= Dependencies.core)
  )

  lazy val metricsReporting = Project(
    id = "service-container-metrics-reporting",
    base = file("./service-container-metrics-reporting"),
    settings = moduleSettings
        ++ Set(dependencyOverrides ++= Dependencies.overrrides)
        ++ Seq(libraryDependencies ++= Dependencies.reporting)
  ).dependsOn(container)

  lazy val examples = Project(
    id = "service-container-examples",
    base = file("./service-container-examples"),
    settings = noPublishSettings ++ defaultSettings
        ++ Seq(libraryDependencies ++= Dependencies.examples)
  ).dependsOn(container)

}
