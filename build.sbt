organization := "com.cluda"

name := "streams"

version := "0.0.1"

scalaVersion := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

test in assembly := {}

assemblyJarName in assembly := "streams.jar"

assemblyOutputPath in assembly := file("docker/streams.jar")

mainClass in assembly := Some("com.cluda.coinsignals.streams.Boot")

resolvers += "sonatype-oss-snapshot" at "https://oss.sonatype.org/content/repositories/snapshots" // for xchange snapshots

libraryDependencies ++= {
  val akkaV       = "2.3.14"
  val akkaStreamV = "1.0"
  val scalaTestV  = "2.2.4"
  Seq(
    "com.typesafe.akka"     %%    "akka-actor"                              %     akkaV,
    "com.typesafe.akka"     %%    "akka-slf4j"                              %     akkaV,
    "com.typesafe.akka"     %%    "akka-stream-experimental"                %     akkaStreamV,
    "com.typesafe.akka"     %%    "akka-http-core-experimental"             %     akkaStreamV,
    "com.typesafe.akka"     %%    "akka-http-experimental"                  %     akkaStreamV,
    "com.typesafe.akka"     %%    "akka-http-spray-json-experimental"       %     akkaStreamV,
    "com.typesafe.akka"     %%    "akka-http-testkit-experimental"          %     akkaStreamV,
    "org.scalatest"         %%    "scalatest"                               %     scalaTestV      %     "test",
    "com.github.seratch"    %%    "awscala"                                 %     "0.5.+",
    "com.amazonaws"          %    "aws-java-sdk-sns"                        %     "1.10.+",
    "commons-codec"          %    "commons-codec"                           %     "1.10",
    "org.bitbucket.b_c"      %    "jose4j"                                  %     "0.4.3",
    "ch.qos.logback"         %    "logback-classic"                         %     "1.1.3"
  )
}