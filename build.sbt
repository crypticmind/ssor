
val scalaVersionString = "2.12.2"

val commonSettings = Seq(
  organization := "crypticmind",
  scalaVersion := scalaVersionString,
  fork in run := true,
  cancelable in run := true,
  scalacOptions := Seq(
    "-encoding", "utf8",
    "-g:vars",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-target:jvm-1.8",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Xlog-reflective-calls"
  )
)

lazy val ssor = project
  .in(file("."))
  .aggregate(framework, `example-store`)
  .settings(
    mainClass := None,
    publishArtifact := false
  )

lazy val framework = project
  .in(file("framework"))
  .settings(commonSettings: _*)
  .settings(
    name := "ssor-framework",
    description := "The base framework for SSoR services",
    mainClass := None,
    libraryDependencies := Seq(
      "org.sangria-graphql" %% "sangria"              % "1.2.0",
      "org.sangria-graphql" %% "sangria-spray-json"   % "1.0.0",
      "org.sangria-graphql" %% "sangria-akka-streams" % "1.0.0",
      "com.typesafe.akka"   %% "akka-http"            % "10.0.6",
      "com.typesafe.akka"   %% "akka-http-spray-json" % "10.0.6",
      "de.heikoseeberger"   %% "akka-sse"             % "2.0.0",
      "org.scalatest"       %% "scalatest"            % "3.0.1" % "test",
      // akka-http still depends on 2.4 but should work with 2.5 without problems
      // see https://github.com/akka/akka-http/issues/821
      "com.typesafe.akka"   %% "akka-stream"          % "2.5.1",
      "com.typesafe.akka"   %% "akka-stream-testkit"  % "2.5.1" % "test"
    )
  )

lazy val `example-store` = project
  .in(file("example-store"))
  .dependsOn(framework % "compile->compile;test->test")
  .settings(commonSettings: _*)
  .settings(
    name := "ssor-example-store",
    description := "An example store using the framework",
    mainClass := Some("crypticmind.examplestore.Main"),
    libraryDependencies := Seq(

    )
  )
