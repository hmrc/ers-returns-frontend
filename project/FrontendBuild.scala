import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "ers-returns-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val hmrcTestVersion = "3.3.0"
  private val pegdownVersion = "1.6.0"
  private val pdfboxVersion = "1.8.16"
  private val xmpboxVersion = "1.8.16"
  private val scalaParserCombinatorsVersion = "1.0.7"
  private val scalatestVersion = "3.0.8"
  private val scalatestPlusPlayVersion = "2.0.1"
  private val jsoupVersion = "1.9.2"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-25",
		"uk.gov.hmrc" %% "bootstrap-play-25" % "5.3.0",
		"uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-25",
		"uk.gov.hmrc" %% "play-ui" % "8.11.0-play-25",
		"uk.gov.hmrc" %% "domain" % "5.9.0-play-25",
    "uk.gov.hmrc" %% "http-caching-client" % "9.1.0-play-25",
    "uk.gov.hmrc" %% "play-language" % "4.3.0-play-25",
		"uk.gov.hmrc" %% "auth-client" % "3.0.0-play-25",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.7",
    "org.apache.pdfbox" % "pdfbox" % "1.8.16",
    "org.apache.pdfbox" % "xmpbox" % "1.8.16"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
				"uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
				"org.scalatest" %% "scalatest" % "3.0.8" % scope,
				"org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
				"org.pegdown" % "pegdown" % "1.6.0" % scope,
				"org.jsoup" % "jsoup" % "1.9.2" % scope,
				"com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
				"org.mockito" % "mockito-core" % "3.4.0" % scope,
				"com.github.tomakehurst" % "wiremock-standalone" % "2.27.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
