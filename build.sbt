ThisBuild / useSuperShell := false

addCommandAlias("validate", ";scalafmtCheck;scalafmtSbtCheck;test")

val Http4sVersion = "0.21.4"
val DoobieVersion = "0.9.0"

lazy val root = (project in file("."))
  .settings(
    name := "simple-app",
    organization := "com.kambius",
    version := "1.0.0",
    scalaVersion := "2.13.2",
    libraryDependencies ++= Seq(
      "org.http4s"     %% "http4s-blaze-server"           % Http4sVersion,
      "org.http4s"     %% "http4s-blaze-client"           % Http4sVersion,
      "org.http4s"     %% "http4s-circe"                  % Http4sVersion,
      "org.http4s"     %% "http4s-dsl"                    % Http4sVersion,
      "io.circe"       %% "circe-derivation"              % "0.13.0-M4",
      "io.circe"       %% "circe-literal"                 % "0.13.0",
      "org.tpolecat"   %% "doobie-core"                   % DoobieVersion,
      "org.tpolecat"   %% "doobie-postgres"               % DoobieVersion,
      "ch.qos.logback"  % "logback-classic"               % "1.2.3",
      "org.scalatest"  %% "scalatest"                     % "3.1.2" % Test,
      "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.0" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:existentials",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused:_",
  "-Xlint:_"
)
