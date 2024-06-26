/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import connectors.ErsConnector
import controllers.auth.RequestWithOptionalAuthContext
import models._
import models.upscan.UpscanCsvFilesCallbackList
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.FrontendSessionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.mongo.cache.CacheItem
import utils.Fixtures.ersRequestObject
import utils._
import views.html.{global_error, summary}

import java.time.{Instant, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

class SummaryDeclarationControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ERSFakeApplicationConfig
    with MockitoSugar
    with ErsTestHelper
    with UpscanData
    with GuiceOneAppPerSuite {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)
  implicit val countryCodes: CountryCodes      = mockCountryCodes

  implicit lazy val mat: Materializer = app.materializer
  val globalErrorView: global_error   = app.injector.instanceOf[global_error]
  val summaryView: summary = app.injector.instanceOf[summary]

  val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", ZonedDateTime.now, "2", "2016", "EMI", "EMI")
  val rsc: ErsMetaData       =
    new ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))


  val schemeOrganiser: SchemeOrganiserDetails = new SchemeOrganiserDetails(
    Fixtures.companyName,
    "Add1",
    Option("Add2"),
    Option("Add3"),
    Option("Add4"),
    Option("UK"),
    Option("AA111AA"),
    Option("AB123456"),
    Option("1234567890")
  )
  val groupSchemeInfo: GroupSchemeInfo        = new GroupSchemeInfo(Option("1"), None)
  val gscomp: CompanyDetails                  =
    new CompanyDetails(Fixtures.companyName, "Adress Line 1", None, None, None, None, None, None, None, true)
  val gscomps: CompanyDetailsList             = new CompanyDetailsList(List(gscomp))

	val reportableEvents: ReportableEvents = new ReportableEvents(Some("1"))
	val fileTypeCSV: CheckFileType = new CheckFileType(Some("csv"))
	val fileTypeODS: CheckFileType = new CheckFileType(Some("ods"))
	val csvFilesCallbackList: UpscanCsvFilesCallbackList = incompleteCsvList
	val trustees: TrusteeDetails = new TrusteeDetails("T Name", "T Add 1", None, None, None, None, None, false)
	val trusteesList: TrusteeDetailsList = new TrusteeDetailsList(List(trustees))
	val fileNameODS: String = "test.osd"

  val commonAllDataMap: Map[String, JsValue] = Map(
    "scheme-type"             -> Json.toJson("1"),
    "portal-scheme-ref"       -> Json.toJson("CSOP - MyScheme - XA1100000000000 - 2014/15"),
    "alt-activity"            -> Json.toJson(new AltAmendsActivity("1")),
    "scheme-organiser"        -> Json.toJson(schemeOrganiser),
    "group-scheme-controller" -> Json.toJson(groupSchemeInfo),
    "group-scheme-companies"  -> Json.toJson(gscomps),
    "trustees"                -> Json.toJson(trusteesList),
    "ReportableEvents"        -> Json.toJson(reportableEvents),
    "ErsMetaData"             -> Json.toJson(rsc)
  )

  class TestSessionService(fetchAllMapVal: String) extends FrontendSessionService(mockSessionRepository, mockFileValidatorService, mockAppConfig) {

    override def getAllData(bundleRef: String, ersMetaData: ErsMetaData)(implicit ec: ExecutionContext, request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[ErsSummary] =
      Future.successful(
        new ErsSummary(
          "testbundle",
          "false",
          None,
          ZonedDateTime.now,
          ersMetaData,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None
        )
      )

    @throws(classOf[NoSuchElementException])
    override def fetchAll()(implicit request: Request[_]): Future[CacheItem] =
      fetchAllMapVal match {
        case "e" => Future(throw new NoSuchElementException)
        case "withSchemeTypeSchemeRef" =>
          val data = commonAllDataMap.view.filterKeys(Seq("scheme-type", "portal-scheme-ref").contains(_)).toMap
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
        case "withAll" =>
          val findList     = Seq(
            "scheme-organiser",
            "group-scheme-controller",
            "group-scheme-companies",
            "trustees",
            "ReportableEvents",
            "ErsMetaData"
          )
          val addList =
            Map("check-file-type" -> Json.toJson(fileTypeCSV), "check-csv-files" -> Json.toJson(mockErsUtil.CSV_FILES_CALLBACK_LIST))
          val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
        case "noGroupSchemeInfo" =>
          val findList =
            Seq("scheme-organiser", "group-scheme-companies", "trustees", "ReportableEvents", "ErsMetaData")
          val addList =
            Map("check-file-type" -> Json.toJson(fileTypeCSV), "check-csv-files" -> Json.toJson(mockErsUtil.CSV_FILES_CALLBACK_LIST))
          val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
        case "odsFile" =>
          val findList = Seq(
            "scheme-type",
            "portal-scheme-ref",
            "alt-activity",
            "scheme-organiser",
            "group-scheme-companies",
            "trustees",
            "ReportableEvents",
            "ErsMetaData"
          )
          val addList = Map("check-file-type" -> Json.toJson(fileTypeODS), "file-name" -> Json.toJson(fileNameODS))
          val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
        case "withAllNillReturn" =>
          val reportableEvents: ReportableEvents = new ReportableEvents(Some(OPTION_NIL_RETURN))
          val fileType: CheckFileType = new CheckFileType(None)
          val findList =
            Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
          val addList = Map(
            "ReportableEvents" -> Json.toJson(reportableEvents),
            "check-file-type"  -> Json.toJson(fileType),
            "check-csv-files"  -> Json.toJson(mockErsUtil.CSV_FILES_CALLBACK_LIST)
          )
          val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
        case "withAllCSVFile" =>
          val reportableEvents: ReportableEvents = new ReportableEvents(Some(OPTION_UPLOAD_SPREEDSHEET))
          val fileType: CheckFileType = new CheckFileType(Some(OPTION_CSV))
          val findList =
            Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
          val addList = Map(
            "ReportableEvents" -> Json.toJson(reportableEvents),
            "check-file-type"  -> Json.toJson(fileType),
            "check-csv-files"  -> Json.toJson(mockErsUtil.CSV_FILES_CALLBACK_LIST)
          )
          val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
        case "withAllODSFile" =>
          val reportableEvents: ReportableEvents = new ReportableEvents(Some(OPTION_UPLOAD_SPREEDSHEET))
          val fileType: CheckFileType = new CheckFileType(Some(OPTION_ODS))
          val findList =
            Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
          val addList = Map(
            "ReportableEvents" -> Json.toJson(reportableEvents),
            "check-file-type" -> Json.toJson(fileType),
            "check-csv-files" -> Json.toJson(mockErsUtil.CSV_FILES_CALLBACK_LIST),
            "file-name" -> Json.toJson(fileNameODS)
          )
          val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
          val ci: CacheItem = CacheItem("id1", Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
          Future.successful(ci)
      }
  }

  lazy val ersConnector: ErsConnector = new ErsConnector(mockHttp, mockAppConfig) {
    override lazy val ersUrl = "ers-returns"
    override lazy val validatorUrl = "ers-file-validator"
    override def connectToEtmpSapRequest(
      schemeRef: String
    )(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[String] = Future(
      "1234567890"
    )
  }

  def buildFakeSummaryDeclarationController(fetchMapVal: String = "e"): SummaryDeclarationController =
    new SummaryDeclarationController(
      mockMCC,
      ersConnector,
      new TestSessionService(fetchMapVal),
      globalErrorView,
      summaryView,
      testAuthAction
    ) {
      when(
        mockHttp.POST[ValidatorData, HttpResponse](
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      ).thenReturn(Future.successful(HttpResponse(OK, "")))
    }

  "Calling SummaryDeclarationController.summaryDeclarationPage (GET) without authentication" should {
    "give a redirect status (to company authentication frontend)" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeSummaryDeclarationController()
      val result = controllerUnderTest.summaryDeclarationPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication missing elements in the cache" should {
    "direct to ers errors page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      contentAsString(
        controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(authRequest)
      ) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (Nil Return) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("withAllNillReturn")
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (CSV File Upload) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("withAllCSVFile")
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (ODS File Upload) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("withAllODSFile")
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements in the cache (ODS)" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("odsFile")
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (no group scheme info) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("noGroupSchemeInfo")
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }
  }
}
