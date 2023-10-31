import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  val pdfboxVersion    = "2.0.29"
  val openHtmlVersion  = "1.0.10"
  val bootstrapVersion = "7.22.0"

  val compile: Seq[ModuleID] = Seq(
    guice,
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "7.24.0-play-28",
    "uk.gov.hmrc"             %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc"             %% "domain"                     % "8.3.0-play-28",
    "uk.gov.hmrc"             %% "http-caching-client"        % "10.0.0-play-28",
    "org.apache.pdfbox"       %  "pdfbox"                     % pdfboxVersion,
    "org.apache.pdfbox"       %  "xmpbox"                     % pdfboxVersion,
    "org.apache.xmlgraphics"  %  "batik-transcoder"           % "1.17",
    "org.apache.xmlgraphics"  %  "batik-codec"                % "1.17",
    "com.typesafe.play"       %% "play-json-joda"             % "2.9.4",
    "com.openhtmltopdf"       %  "openhtmltopdf-core"         % openHtmlVersion,
    "com.openhtmltopdf"       %  "openhtmltopdf-pdfbox"       % openHtmlVersion,
    "com.openhtmltopdf"       %  "openhtmltopdf-svg-support"  % openHtmlVersion,
    "commons-codec"           %  "commons-codec"              % "1.15"
  )

  val test: Seq[ModuleID]      = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28" % bootstrapVersion,
    "org.scalatest"           %% "scalatest"              % "3.2.17",
    "org.scalatestplus"       %% "mockito-4-11"           % "3.2.17.0",
    "com.vladsch.flexmark"     %  "flexmark-all"            % "0.64.8",
    "org.pegdown"             %  "pegdown"                % "1.6.0",
    "org.jsoup"               %  "jsoup"                  % "1.16.2",
    "org.wiremock"            % "wiremock-standalone"     % "3.0.2"
  ).map(_ % Test)

  val overrides: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
