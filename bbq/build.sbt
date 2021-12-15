lazy val akkaHttpVersion = "10.2.7"
lazy val akkaVersion = "2.6.17"
lazy val slickVersion = "3.3.3"
lazy val postgresVersion = "42.3.1"

lazy val root = (project in file(".")).
    settings(
        inThisBuild(List(
            organization := "misis",
            scalaVersion := "2.13.4"
        )),
        name := "BBQ",
        libraryDependencies ++= Seq(
            "com.typesafe.akka"  %% "akka-http"            % akkaHttpVersion,
            "com.typesafe.akka"  %% "akka-http-spray-json" % akkaHttpVersion,
            "com.typesafe.akka"  %% "akka-actor-typed"     % akkaVersion,
            "com.typesafe.akka"  %% "akka-stream"          % akkaVersion,
            "ch.qos.logback"     % "logback-classic"       % "1.2.3",
            "com.typesafe.slick" %% "slick"                % slickVersion,
            "com.typesafe.slick" %% "slick-hikaricp"       % slickVersion,
            "org.postgresql"     % "postgresql"            % postgresVersion,

            "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
            "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
            "org.scalatest"     %% "scalatest"                % "3.1.4"         % Test,
            "com.opentable.components" % "otj-pg-embedded"    % "0.13.3"        % Test
        )
    )
