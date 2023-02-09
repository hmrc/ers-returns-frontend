import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val pdfboxVersion = "2.0.27"
  val openHtmlVersion = "1.0.10"

  val compile: Seq[ModuleID] = Seq(
    guice,
    ws,
    "uk.gov.hmrc"            %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"            %% "bootstrap-frontend-play-28" % "7.13.0",
    "uk.gov.hmrc"            %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"            %% "http-caching-client"        % "10.0.0-play-28",
    "uk.gov.hmrc"            %% "time"                       % "3.25.0",
    "uk.gov.hmrc"            %% "play-frontend-hmrc"         % "6.2.0-play-28",
    "org.scala-lang.modules" %% "scala-parser-combinators"   % "2.2.0",
    "org.apache.pdfbox"      %  "pdfbox"                     % pdfboxVersion,
    "org.apache.pdfbox"      %  "xmpbox"                     % pdfboxVersion,
    "org.apache.xmlgraphics" %  "batik-transcoder"           % "1.16",
    "org.apache.xmlgraphics" %  "batik-codec"                % "1.16",
    "com.typesafe.play"      %% "play-json-joda"             % "2.9.4",
    "com.openhtmltopdf"      %  "openhtmltopdf-core"         % openHtmlVersion,
    "com.openhtmltopdf"      %  "openhtmltopdf-pdfbox"       % openHtmlVersion,
    "com.openhtmltopdf"      %  "openhtmltopdf-svg-support"  % openHtmlVersion,
    "commons-codec"          %  "commons-codec"              % "1.15"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest"          %% "scalatest"           % "3.2.15"            % scope,
        "org.scalatestplus"      %% "mockito-3-4"         % "3.2.10.0"          % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"  % "5.1.0"             % scope,
        "com.vladsch.flexmark"   %  "flexmark-all"        % "0.62.2"            % scope,
        "org.pegdown"            %  "pegdown"             % "1.6.0"             % scope,
        "org.jsoup"              %  "jsoup"               % "1.15.3"            % scope,
        "com.typesafe.play"      %% "play-test"           % PlayVersion.current % scope,
        "com.github.tomakehurst" %  "wiremock-standalone" % "2.27.2"            % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()

  val overrides = Seq(
    "org.scala-lang.modules"      %%    "scala-parser-combinators"      %   "2.1.1"
  )
}
