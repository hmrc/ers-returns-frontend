import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    guice,
    ws,
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-27",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.14.0",
    "uk.gov.hmrc" %% "domain" % "6.2.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-27",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-27",
    "uk.gov.hmrc" %% "time" % "3.25.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "1.14.0-play-27",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0",
    "org.apache.pdfbox" % "pdfbox" % "2.0.24",
    "org.apache.pdfbox" % "xmpbox" % "2.0.24",
    "com.typesafe.play" %% "play-json-joda" % "2.9.2"
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
        "org.jsoup" % "jsoup" % "1.14.2" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.12.4" % scope,
        "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

}
