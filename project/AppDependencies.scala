import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    guice,
    ws,
    "uk.gov.hmrc"                 %%    "play-partials"                 %   "8.2.0-play-28",
    "uk.gov.hmrc"                 %%    "bootstrap-frontend-play-28"    %   "5.18.0",
    "uk.gov.hmrc"                 %%    "domain"                        %   "6.2.0-play-28",
    "uk.gov.hmrc"                 %%    "http-caching-client"           %   "9.5.0-play-28",
    "uk.gov.hmrc"                 %%    "play-language"                 %   "5.1.0-play-28",
    "uk.gov.hmrc"                 %%    "time"                          %   "3.25.0",
    "uk.gov.hmrc"                 %%    "play-frontend-hmrc"            %   "1.31.0-play-28",
    "org.scala-lang.modules"      %%    "scala-parser-combinators"      %   "2.1.0",
    "org.apache.pdfbox"           %     "pdfbox"                        %   "2.0.24",
    "org.apache.pdfbox"           %     "xmpbox"                        %   "2.0.24",
    "com.typesafe.play"           %%    "play-json-joda"                %   "2.9.2"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest"           %%    "scalatest"             %     "3.2.10"        % scope,
        "org.scalatestplus"       %%    "mockito-3-4"           %     "3.2.10.0"      % scope,
        "org.scalatestplus.play"  %%    "scalatestplus-play"    %     "5.1.0"         % scope,
        "com.vladsch.flexmark"     %     "flexmark-all"           %     "0.62.2"        % scope,
        "org.pegdown"             %     "pegdown"               %     "1.6.0"         % scope,
        "org.jsoup"               %     "jsoup"                 %     "1.14.3"        % scope,
        "com.typesafe.play"       %%    "play-test"             %     PlayVersion.current % scope,
        "com.github.tomakehurst"  %     "wiremock-standalone"   %     "2.27.2"        % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

}
