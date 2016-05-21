organization := "com.cluda"

name := "streams"

version := "0.1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

test in assembly := {}

assemblyJarName in assembly := "streams.jar"

assemblyOutputPath in assembly := file("docker/streams.jar")

mainClass in assembly := Some("com.cluda.tradersbit.streams.Boot")

resolvers += "sonatype-oss-snapshot" at "https://oss.sonatype.org/content/repositories/snapshots" // for xchange snapshots

libraryDependencies ++= {
  val akkaV       = "2.4.6"
  Seq(
    "com.typesafe.akka"     %%    "akka-actor"                              %     akkaV,
    "com.typesafe.akka"     %%    "akka-slf4j"                              %     akkaV,
    "com.typesafe.akka"     %%    "akka-stream"                             %     akkaV,
    "com.typesafe.akka"     %%    "akka-http-core"                          %     akkaV,
    "com.typesafe.akka"     %%    "akka-http-experimental"                  %     akkaV,
    "com.typesafe.akka"     %%    "akka-http-spray-json-experimental"       %     akkaV,
    "com.typesafe.akka"     %%    "akka-http-testkit"                       %     akkaV,
    "org.scalatest"         %%    "scalatest"                               %     "2.2.6"      %     "test",
    "com.github.seratch"    %%    "awscala"                                 %     "0.5.5",
    "com.amazonaws"          %    "aws-java-sdk-sns"                        %     "1.10.77",
    "commons-codec"          %    "commons-codec"                           %     "1.10",
    "org.bitbucket.b_c"      %    "jose4j"                                  %     "0.5.1",
    "ch.qos.logback"         %    "logback-classic"                         %     "1.1.7"
  )
}