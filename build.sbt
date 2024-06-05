import uk.gov.hmrc.*
import DefaultBuildSettings.*
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val microservice = Project("ers-returns-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scoverageSettings,
    scalaSettings,
    defaultSettings(),
    scalaVersion := "2.13.14",
    libraryDependencies ++= AppDependencies(),
    dependencyOverrides ++= AppDependencies.overrides,
    Test / parallelExecution := false,
    Test / fork := false,
    Runtime / fork := true,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    TwirlKeys.templateImports +=
      "models.upscan.{UpscanInitiateResponse, UpscanCsvFilesCallbackList, UpscanCsvFilesCallback}",
    routesImport += "models.upscan.UploadId",
    PlayKeys.playDefaultPort := 9290,
    majorVersion := 1
  )

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s",
  "-Wconf:cat=unused-imports&src=html/.*:s"
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;prod.*;app.*;.*BuildInfo.*;view.*;.*Metrics;.*config;.*Global;prod.Routes;internal.Routes;testOnlyDoNotUseInAppConf.Routes;.*Configuration;config.AuditFilter;config.LoggingFilter;.*config.WSHttp;utils.HMACUtil;controllers.ERSGovernmentGateway;controllers.ERSReturnBaseController;.*Routes.*;.*repositories.*;.*models.*;.*RoutesPrefix;",
    ScoverageKeys.coverageMinimumStmtTotal := 89,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
