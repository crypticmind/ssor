
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
  .aggregate(backend, frontend)
  .settings(
    mainClass := None,
    publishArtifact := false
  )

lazy val backend = project
  .in(file("backend"))
  .settings(commonSettings: _*)
  .settings(
    name := "ssor-backend",
    description := "The application backend",
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

lazy val frontend = project
  .in(file("frontend"))
  .dependsOn(backend)
  .settings(commonSettings: _*)
  .settings(
    name := "ssor-frontend",
    description := "The application frontend",
    mainClass := None,
    libraryDependencies := Seq(
    )
  )
