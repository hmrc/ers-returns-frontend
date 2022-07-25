
import uk.gov.hmrc._
import DefaultBuildSettings._
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "ers-returns-frontend"

val silencerVersion = "1.7.9"

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*Service.*;models/.data/..*;prod.*;app.*;.*BuildInfo.*;view.*;.*Connector.*;.*Metrics;.*config;.*Global;prod.Routes;internal.Routes;testOnlyDoNotUseInAppConf.Routes;.*Configuration;config.AuditFilter;config.LoggingFilter;.*config.WSHttp;utils.HMACUtil;models.RequestObject;models.fileDataTracking;controllers.ERSGovernmentGateway;controllers.ERSReturnBaseController;",
    ScoverageKeys.coverageMinimumStmtTotal := 89,
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
    scalaVersion := "2.12.16",
    libraryDependencies ++= AppDependencies.apply(),
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
    majorVersion := 1,
    integrationTestSettings(),
    // ***************
    // Use the silencer plugin to suppress warnings
    // You may turn it on for `views` too to suppress warnings from unused imports in compiled twirl templates, but this will hide other warnings.
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
    scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
    // Suppress warnings due to mongo dates using `$date` in their Json representation
    scalacOptions += "-P:silencer:globalFilters=possible missing interpolator: detected interpolated identifier `\\$date`",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .configs(IntegrationTest)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.12.0" force(),
)
