
import uk.gov.hmrc._
import DefaultBuildSettings._
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "ers-returns-frontend"

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*Service.*;models/.data/..*;prod.*;app.*;.*BuildInfo.*;view.*;.*Connector.*;.*Metrics;.*config;.*Global;prod.Routes;testOnlyDoNotUseInAppConf.Routes;.*Configuration;config.AuditFilter;config.LoggingFilter;.*config.WSHttp;utils.HMACUtil;models.RequestObject;models.fileDataTracking;controllers.ERSGovernmentGateway;controllers.ERSReturnBaseController;",
    ScoverageKeys.coverageMinimum := 89,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scoverageSettings,
    publishingSettings,
    scalaSettings,
    defaultSettings(),
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies.apply(),
    parallelExecution in Test := false,
    fork in Test := false,
    fork in Runtime := true,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    TwirlKeys.templateImports +=
      "models.upscan.{UpscanInitiateResponse, UpscanCsvFilesCallbackList, UpscanCsvFilesCallback}",
    routesImport += "models.upscan.UploadId",
    PlayKeys.playDefaultPort := 9290,
    majorVersion := 1,
    integrationTestSettings(),
  )
  .configs(IntegrationTest)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.helpers._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
