import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  val pdfboxVersion    = "2.0.32"
  val openHtmlVersion  = "1.0.10"

  val bootstrapVersion = "9.11.0"
  val mongoVersion     = "2.6.0"

  val compile: Seq[ModuleID] = Seq(
    guice,
    ws,
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30" % "11.13.0",
    "uk.gov.hmrc"             %% "play-partials-play-30"      % "10.0.0",
    "uk.gov.hmrc"             %% "domain-play-30"             % "10.0.0",
    "org.apache.pdfbox"       %  "pdfbox"                     % pdfboxVersion,
    "org.apache.pdfbox"       %  "xmpbox"                     % pdfboxVersion,
    "org.apache.xmlgraphics"  %  "batik-transcoder"           % "1.18",
    "org.apache.xmlgraphics"  %  "batik-codec"                % "1.18",
    "com.openhtmltopdf"       %  "openhtmltopdf-core"         % openHtmlVersion,
    "com.openhtmltopdf"       %  "openhtmltopdf-pdfbox"       % openHtmlVersion,
    "com.openhtmltopdf"       %  "openhtmltopdf-svg-support"  % openHtmlVersion,
    "commons-codec"           %  "commons-codec"              % "1.18.0",
    "org.codehaus.janino"     %  "janino"                     % "3.1.12",
    "commons-io"              %  "commons-io"                 % "2.18.0"
  )

  val test: Seq[ModuleID]      = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % mongoVersion,
    "org.scalatest"           %% "scalatest"               % "3.2.19",
    "org.scalatestplus"       %% "mockito-5-10"            % "3.2.18.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.8",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.19.1",
    "org.wiremock"            %  "wiremock-standalone"     % "3.12.1"
  ).map(_ % Test)

  val overrides: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
    "com.ibm.icu"            %  "icu4j"                    % "69.1"   //To resolve dependency clash between flexmark v0.64.4+ and play-language to run accessibility tests, remove when versions align
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
