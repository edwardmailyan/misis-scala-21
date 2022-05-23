lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion = "2.6.19"

lazy val common = ProjectRef(base = file("../reactive-pizza/common"), id = "common")

lazy val root = (project in file("."))
    .dependsOn(common)
    .settings(
        inThisBuild(List(
            organization := "ru.misis",
            scalaVersion := "2.13.4"
        )),
        name := "menu",
        libraryDependencies ++= Seq(
            "ch.qos.logback" % "logback-classic" % "1.2.3",
            "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.17.2"

        )
    )
