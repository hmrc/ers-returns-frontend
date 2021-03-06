import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    guice,
    ws,
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.6.0",
    "uk.gov.hmrc" %% "domain" % "6.1.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-27",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-27",
    "uk.gov.hmrc" %% "time" % "3.25.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "0.79.0-play-27",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "org.apache.pdfbox" % "pdfbox" % "1.8.16",
    "org.apache.pdfbox" % "xmpbox" % "1.8.16",
    "com.typesafe.play" %% "play-json-joda" % "2.6.14"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % "3.0.9" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.13.1" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.11.2" % scope,
        "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

}
