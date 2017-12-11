name := "OSDSC"

version := "1.0"

organization := "edu.uga.quinngroup"

scalaVersion := "2.10.5"


libraryDependencies ++= Seq(


"org.scalanlp" %% "breeze" % "0.11.2",

   "org.scalanlp" %% "breeze-natives" % "0.11.2",

    "org.scalanlp" %% "breeze-viz" % "0.11.2"
)

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.2" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-mllib" % "1.6.2" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.2" % "test" classifier "tests"

libraryDependencies += "org.apache.spark" %% "spark-mllib" % "1.6.2" % "test" classifier "tests"
