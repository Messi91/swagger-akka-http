name := "swagger-akka-http"

version := "1.0"

scalaVersion := "2.11.12"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
    "de.heikoseeberger" %% "akka-http-circe" % "1.5.2",
    "io.circe" %% "circe-generic" % "0.3.0",
    "io.circe" %% "circe-java8" % "0.3.0",
    "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.6.2"
  )
}
