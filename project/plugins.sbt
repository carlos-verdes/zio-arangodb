ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
) //Needed to resolve version conflict for scala-xml https://github.com/sbt/sbt/issues/7007

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
addSbtPlugin("com.mayreh" % "sbt-thank-you-stars" % "0.2")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn" % "0.1.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.14.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.3.11")
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "2.0.9")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

addDependencyTreePlugin
