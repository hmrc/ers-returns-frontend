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

import models._
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
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
    with ErsTestHelper
    with ERSFakeApplicationConfig
    with BeforeAndAfterEach
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
  implicit val countryCodes: CountryCodes = mockCountryCodes

  implicit lazy val materializer: Materializer = app.materializer
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val summaryView: summary = app.injector.instanceOf[summary]

  val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", ZonedDateTime.now, "2", "2016", "EMI", "EMI")
  val rsc: ErsMetaData =
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
  val groupSchemeInfo: GroupSchemeInfo = new GroupSchemeInfo(Option("1"), None)
  val gscomp: CompanyDetails =
    new CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, Some("UK"), None, None, None)
  val gscomps: CompanyDetailsList = new CompanyDetailsList(List(gscomp))

  val alterationAmends: AlterationAmends = new AlterationAmends(
    Option("1"),
    Option("1"),
    Option("1"),
    Option("1"),
    Option("1")
  )

  val reportableEvents: ReportableEvents = new ReportableEvents(Some("1"))
  val fileTypeCSV: CheckFileType = new CheckFileType(Some("csv"))
  val fileTypeODS: CheckFileType = new CheckFileType(Some("ods"))
  val csvFileCallBackList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId("abcd"), "id0", UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "http://test.gov.uk"))))
  val csvFilesCallbackList: UpscanCsvFilesCallbackList = incompleteCsvList
  val trustees: TrusteeDetails = new TrusteeDetails("T Name", "T Add 1", None, None, None, None, None, false)
  val trusteesList: TrusteeDetailsList = new TrusteeDetailsList(List(trustees))
  val fileNameODS: String = "test.osd"

  val company: CompanyDetails =
    CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, Some("UK"), None, None, None)
  lazy val companyDetailsList: CompanyDetailsList = CompanyDetailsList(List(company, company))
  lazy val companyDetailsListSingle: CompanyDetailsList = CompanyDetailsList(List(company))

  val commonAllDataMap: Map[String, JsValue] = Map(
    "scheme-type" -> Json.toJson("1"),
    "portal-scheme-ref" -> Json.toJson("CSOP - MyScheme - XA1100000000000 - 2014/15"),
    "alt-activity" -> Json.toJson(new AltAmendsActivity("1")),
    "scheme-organiser" -> Json.toJson(schemeOrganiser),
    "group-scheme-controller" -> Json.toJson(groupSchemeInfo),
    "group-scheme-companies" -> Json.toJson(gscomps),
    "trustees" -> Json.toJson(trusteesList),
    "reportable-events" -> Json.toJson(reportableEvents),
    "alt-amends-cache-controller" -> Json.toJson(alterationAmends),
    "ErsMetaData" -> Json.toJson(rsc)
  )

  val csopRequestObject: RequestObject = ersRequestObject.copy(schemeName = Some("CSOP"), schemeType = Some("CSOP"))

  def setCacheItem(id: String, findList: Seq[String], addList: Map[String, JsValue]): CacheItem = {
    val data = commonAllDataMap.view.filterKeys(findList.contains(_)).toMap ++ addList
    CacheItem(id, Json.toJson(data).as[JsObject], Instant.now(), Instant.now())
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockErsUtil, mockSessionService)
    when(mockErsUtil.SCHEME_ORGANISER_CACHE).thenReturn("scheme-organiser")
    when(mockErsUtil.GROUP_SCHEME_COMPANIES).thenReturn("group-scheme-companies")
    when(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER).thenReturn("group-scheme-controller")
    when(mockErsUtil.ALT_AMENDS_CACHE_CONTROLLER).thenReturn("alt-amends-cache-controller")
    when(mockErsUtil.REPORTABLE_EVENTS).thenReturn("reportable-events")
    when(mockErsUtil.FILE_TYPE_CACHE).thenReturn("check-file-type")
    when(mockErsUtil.OPTION_CSV).thenReturn("csv")
    when(mockErsUtil.OPTION_ODS).thenReturn("ods")
    when(mockErsUtil.OPTION_UPLOAD_SPREEDSHEET).thenReturn("1")
    when(mockErsUtil.OPTION_NIL_RETURN).thenReturn("2")
    when(mockErsUtil.CHECK_CSV_FILES).thenReturn("check-csv-files")
    when(mockErsUtil.CSV_FILES_CALLBACK_LIST).thenReturn("csv-file-callback-List")
    when(mockErsUtil.FILE_NAME_CACHE).thenReturn("file-name")
    when(mockErsUtil.ALT_AMENDS_ACTIVITY).thenReturn("alt-activity")
    when(mockErsUtil.TRUSTEES_CACHE).thenReturn("trustees")
    when(mockErsUtil.ERS_REQUEST_OBJECT).thenReturn("ers-request-object")
    when(mockErsUtil.OPTION_MANUAL).thenReturn("man")
    when(mockErsUtil.DEFAULT).thenReturn("")
    when(mockErsUtil.SCHEME_CSOP).thenReturn("1")
    when(mockErsUtil.SCHEME_EMI).thenReturn("2")
    when(mockErsUtil.SCHEME_OTHER).thenReturn("3")
    when(mockErsUtil.SCHEME_SAYE).thenReturn("4")
    when(mockErsUtil.SCHEME_SIP).thenReturn("5")
    when(mockErsUtil.OPTION_YES).thenReturn("1")
    when(mockErsUtil.OPTION_NO).thenReturn("2")
    when(mockErsUtil.getPageElement(any(), any(), any(), any())(any())).thenCallRealMethod()
    when(mockErsUtil.MSG_CSOP).thenReturn(".csop.")
    when(mockErsUtil.PAGE_GROUP_SUMMARY).thenReturn("ers_group_summary")
    when(mockErsUtil.PAGE_SUMMARY_DECLARATION).thenReturn("ers_summary_declaration")
    when(mockErsUtil.PAGE_CHOOSE).thenReturn("ers_choose")
    when(mockErsUtil.PAGE_ALT_ACTIVITY).thenReturn("ers_alt_activity")
    when(mockErsUtil.PAGE_GROUP_ACTIVITY).thenReturn("ers_group_activity")
    when(mockErsUtil.PAGE_ALT_AMENDS).thenReturn("ers_alt_amends")
  }

  lazy val testSummaryDeclarationController: SummaryDeclarationController = new SummaryDeclarationController(
    mockMCC,
    mockErsConnector,
    mockSessionService,
    globalErrorView,
    summaryView,
    testAuthAction
  )

  "Calling SummaryDeclarationController.summaryDeclarationPage (GET) without authentication" should {
    def summaryDeclarationControllerHandler(request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testSummaryDeclarationController.summaryDeclarationPage().apply(request))

    "give a redirect status (to company authentication frontend)" in {
      setUnauthorisedMocks()
      summaryDeclarationControllerHandler(Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication, missing elements in the cache" should {
    "direct to ers errors page" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.failed(new NoSuchElementException))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (Nil Return) in the cache" should {
    "show the summary declaration page" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val findList =
        Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
      val addList = Map(
        "reportable-events" -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
        "alt-activity"      -> Json.toJson(new AltAmendsActivity("2"))
      )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (CSV File Upload) in the cache" should {
    "show the summary declaration page" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
      val addList = Map(
        "reportable-events" -> Json.toJson(new ReportableEvents(Some(mockErsUtil.OPTION_UPLOAD_SPREEDSHEET))),
        "check-file-type"   -> Json.toJson(fileTypeCSV),
        "check-csv-files"   -> Json.toJson(csvFileCallBackList),
        "alt-activity"      -> Json.toJson(new AltAmendsActivity("2"))
      )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (ODS File Upload) in the cache" should {
    "show the summary declaration page" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
      val addList = Map(
        "reportable-events" -> Json.toJson(new ReportableEvents(Some(mockErsUtil.OPTION_UPLOAD_SPREEDSHEET))),
        "check-file-type"   -> Json.toJson(fileTypeODS),
        "file-name"         -> Json.toJson(fileNameODS),
        "alt-activity"      -> Json.toJson(new AltAmendsActivity("2"))
      )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements in the cache (ODS)" should {
    "show the summary declaration page" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList = Seq(
        "scheme-type", "portal-scheme-ref", "scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "reportable-events", "ErsMetaData"
      )
      val addList = Map(
        "check-file-type" -> Json.toJson(fileTypeODS),
        "file-name"       -> Json.toJson(fileNameODS),
        "alt-activity"    -> Json.toJson(new AltAmendsActivity("2"))
      )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (no group scheme info) in the cache" should {
    "show the error page if group scheme info is missing" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "group-scheme-companies", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events" -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "alt-activity"      -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and group scheme set to 'Yes" should {
    "show the global error page if companies empty but group scheme set to 'Yes'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "group-scheme-controller", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events" -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "alt-activity"      -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "show the summary declaration page if companies filled but group scheme set to 'Yes'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events" -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "alt-activity"      -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and group scheme set to 'No'" should {
    "show the global error page if companies filled but group scheme set to 'No'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "group-scheme-companies", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None)),
          "alt-activity"            -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "show the summary declaration page if companies empty but group scheme set to 'No'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-organiser", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None)),
          "alt-activity"            -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and alt activity set to 'Yes'" should {
    "show the global error page if alt amends empty but alt activity set to 'Yes'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "alt-activity", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(csopRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "show the summary declaration page if alt amends filled but alt activity set to 'Yes'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "alt-activity", "alt-amends-cache-controller", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(csopRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and alt activity set to 'No'" should {
    "show the global error page if alt amends filled but alt activity set to 'No'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "alt-amends-cache-controller", "scheme-organiser", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None)),
          "alt-activity"            -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(csopRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "show the summary declaration page if alt amends empty but alt activity set to 'No'" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None)),
          "alt-activity"            -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(csopRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and alt activity empty" should {
    "show the summary declaration page if alt amends empty and alt activity empty" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(csopRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }

    "show the global error page if alt amends filled but alt activity empty" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "alt-amends-cache-controller", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(csopRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and none alt activity valid scheme" should {
    "show the summary declaration page if alt amends empty but alt activity 'No' for non compliant scheme" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None)),
          "alt-activity"            -> Json.toJson(new AltAmendsActivity("2"))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.summary.page_title")
    }

    "show the global error page if alt amends filled but alt activity empty for non compliant scheme" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val findList =
        Seq("scheme-type", "portal-scheme-ref", "scheme-organiser", "alt-amends-cache-controller", "trustees", "ErsMetaData")
      val addList =
        Map(
          "reportable-events"       -> Json.toJson(new ReportableEvents(Some(OPTION_NIL_RETURN))),
          "group-scheme-controller" -> Json.toJson(new GroupSchemeInfo(Option("2"), None))
        )

      when(mockErsUtil.buildEntitySummary(any())).thenReturn("Company Name, Add1, Add2, Add3, Add4, UK, AA111AA, AB123456, 1234567890")
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(setCacheItem("id1", findList, addList)))

      val result = testSummaryDeclarationController.showSummaryDeclarationPage(ersRequestObject)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }
  }
}
