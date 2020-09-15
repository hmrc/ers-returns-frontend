import play.routes.compiler.{InjectedRoutesGenerator, StaticRoutesGenerator}
import play.sbt.routes.RoutesKeys.{routesGenerator, routesImport}
import play.sbt.PlayImport.PlayKeys
import play.twirl.sbt.Import.TwirlKeys
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import TestPhases._
  import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}
  import uk.gov.hmrc.versioning.SbtGitVersioning
  import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

  val appName: String

  lazy val appDependencies: Seq[ModuleID] = ???
  lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  lazy val playSettings: Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*Service.*;models/.data/..*;prod.*;app.*;.*BuildInfo.*;view.*;.*Connector.*;.*Metrics;.*config;.*Global;prod.Routes;testOnlyDoNotUseInAppConf.Routes;.*Configuration;config.AuditFilter;config.LoggingFilter;.*config.WSHttp;utils.HMACUtil;models.RequestObject;models.fileDataTracking;controllers.ERSGovernmentGateway;controllers.ERSReturnBaseController;",
      ScoverageKeys.coverageMinimum := 70,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(plugins: _*)
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .enablePlugins(SbtDistributablesPlugin)
    .settings(playSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(publishingSettings: _*)
    .settings(scalaSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.12",
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      fork in Runtime := true,
      retrieveManaged := true,
      routesGenerator := InjectedRoutesGenerator,
      TwirlKeys.templateImports +=
        "models.upscan.{UpscanInitiateResponse, UpscanCsvFilesCallbackList, UpscanCsvFilesCallback}",
      routesImport += "models.upscan.UploadId"
    )
    .settings(Repositories.playPublishingSettings: _*)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .configs(IntegrationTest)
    .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
    .settings(integrationTestSettings())
    .settings(majorVersion := 1)
    .settings(PlayKeys.playDefaultPort := 9290)
}

private object TestPhases {

  val allPhases = "tt->test;test->test;test->compile;compile->compile"
  val allItPhases = "tit->it;it->it;it->compile;compile->compile"

  lazy val TemplateTest = config("tt") extend Test
  lazy val TemplateItTest = config("tit") extend IntegrationTest

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}

private object Repositories {

  import uk.gov.hmrc._
  import PublishingSettings._

  lazy val playPublishingSettings: Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(

    credentials += SbtCredentials,

    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := false
  ) ++
    publishAllArtefacts
}
