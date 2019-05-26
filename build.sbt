name := "green-api"

version := "20190526"

scalaVersion := "2.12.8"

organization := "se.chimps.green"

credentials += Credentials(Path.userHome / ".ivy2" / ".green")

publishTo := Some("se.chimps.green" at "https://yamr.kodiak.se/maven")

publishArtifact in (Compile, packageDoc) := false

resolvers += "se.chimps.green" at "https://yamr.kodiak.se/maven"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.5.22",
	"com.typesafe.akka" %% "akka-cluster" % "2.5.22",
	"com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.22",
	"com.typesafe.akka" %% "akka-cluster-tools" % "2.5.22",
	"se.chimps.green" %% "green-spi" % "20190519"
)
