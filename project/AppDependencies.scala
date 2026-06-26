import sbt.*

object AppDependencies {

  private val openHtmlVersion  = "1.1.34"
  private val bootstrapVersion = "10.7.0"
  private val mongoVersion     = "2.12.0"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"            %% "play-frontend-hmrc-play-30" % "13.9.0",
    "uk.gov.hmrc"            %% "domain-play-30"             % "11.0.0",
    "io.github.openhtmltopdf" % "openhtmltopdf-pdfbox"       % openHtmlVersion,
    "commons-codec"           % "commons-codec"              % "1.21.0",
    "commons-io"              % "commons-io"                 % "2.21.0",
    "io.github.openhtmltopdf" % "openhtmltopdf-svg-support"  % openHtmlVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
