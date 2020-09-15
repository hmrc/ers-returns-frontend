/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import connectors.ErsConnector
import helpers.ErsTestHelper
import metrics.Metrics
import models._
import models.upscan.UpscanCsvFilesCallbackList
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ERSUtil, Fixtures, UpscanData}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class SummaryDeclarationControllerTest extends UnitSpec with ERSFakeApplicationConfig with MockitoSugar with ErsTestHelper with OneAppPerSuite with UpscanData {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
	val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val mat: Materializer = app.materializer
  implicit lazy val messages: Messages = Messages(Lang("en"), messagesApi)

	val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "2", "2016", "EMI", "EMI")
	val rsc: ErsMetaData = new ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

	val schemeOrganiser: SchemeOrganiserDetails = new SchemeOrganiserDetails(Fixtures.companyName,
		"Add1", Option("Add2"), Option("Add3"), Option("Add4"),
		Option("UK"), Option("AA111AA"), Option("AB123456"), Option("1234567890"))
	val groupSchemeInfo: GroupSchemeInfo = new GroupSchemeInfo(Option("1"), None)
	val gscomp: CompanyDetails = new CompanyDetails(Fixtures.companyName, "Adress Line 1", None, None, None, None, None, None, None)
	val gscomps: CompanyDetailsList = new CompanyDetailsList(List(gscomp))

	val reportableEvents: ReportableEvents = new ReportableEvents(Some("1"))
	val fileTypeCSV: CheckFileType = new CheckFileType(Some("csv"))
	val fileTypeODS: CheckFileType = new CheckFileType(Some("ods"))
	val csvFilesCallbackList: UpscanCsvFilesCallbackList = incompleteCsvList
	val trustees: TrusteeDetails = new TrusteeDetails("T Name", "T Add 1", None, None, None, None, None)
	val trusteesList: TrusteeDetailsList = new TrusteeDetailsList(List(trustees))
	val fileNameODS: String = "test.osd"

	val commonAllDataMap: Map[String, JsValue] = Map(
		"scheme-type" -> Json.toJson("1"),
		"portal-scheme-ref" -> Json.toJson("CSOP - MyScheme - XA1100000000000 - 2014/15"),
		"alt-activity" -> Json.toJson(new AltAmendsActivity("1")),
		"scheme-organiser" -> Json.toJson(schemeOrganiser),
		"group-scheme-controller"-> Json.toJson(groupSchemeInfo),
		"group-scheme-companies" -> Json.toJson(gscomps),
		"trustees" -> Json.toJson(trusteesList),
		"ReportableEvents" -> Json.toJson(reportableEvents),
		"ErsMetaData"-> Json.toJson(rsc)
	)

	class TestErsUtil(fetchAllMapVal: String) extends ERSUtil(mockSessionCache, mockShortLivedCache, mockAppConfig){

		override def cache[T](key: String, body: T, cacheId: String)
												 (implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[CacheMap] = {
			Future.successful(CacheMap("fakeId", Map()))
		}

		override def getAllData(bundleRef: String, ersMetaData: ErsMetaData)
													 (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[AnyRef]): Future[ErsSummary] = {
			Future.successful(new ErsSummary("testbundle", "false", None, DateTime.now, ersMetaData, None, None, None, None, None, None, None, None))
		}

		@throws(classOf[NoSuchElementException])
    override def fetchAll(cacheId: String)(implicit hc: HeaderCarrier, request: Request[AnyRef]): Future[CacheMap] = {
			fetchAllMapVal match {
				case "e" => Future(throw new NoSuchElementException)
				case "withSchemeTypeSchemeRef" =>
					val data = commonAllDataMap.filterKeys(Seq("scheme-type", "portal-scheme-ref").contains(_))
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
				case "withAll" =>
					val findList = Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ReportableEvents", "ErsMetaData")
					val addList = Map("check-file-type" -> Json.toJson(fileTypeCSV), "check-csv-files" -> Json.toJson(csvFilesCallbackList))
					val data = commonAllDataMap.filterKeys(findList.contains(_)) ++ addList
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
				case "noGroupSchemeInfo" =>
					val findList = Seq("scheme-organiser", "group-scheme-companies", "trustees", "ReportableEvents", "ErsMetaData")
					val addList = Map("check-file-type" -> Json.toJson(fileTypeCSV), "check-csv-files" -> Json.toJson(csvFilesCallbackList))
					val data = commonAllDataMap.filterKeys(findList.contains(_)) ++ addList
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
				case "odsFile" =>
					val findList = Seq("scheme-type", "portal-scheme-ref", "alt-activity", "scheme-organiser", "group-scheme-companies", "trustees", "ReportableEvents", "ErsMetaData")
					val addList = Map("check-file-type" -> Json.toJson(fileTypeODS), "file-name" -> Json.toJson(fileNameODS))
					val data = commonAllDataMap.filterKeys(findList.contains(_)) ++ addList
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
				case "withAllNillReturn" =>
					val reportableEvents: ReportableEvents = new ReportableEvents(Some(OPTION_NIL_RETURN))
					val fileType: CheckFileType = new CheckFileType(None)
					val findList = Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
					val addList = Map("ReportableEvents" -> Json.toJson(reportableEvents), "check-file-type" -> Json.toJson(fileType), "check-csv-files" -> Json.toJson(csvFilesCallbackList))
					val data = commonAllDataMap.filterKeys(findList.contains(_)) ++ addList
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
				case "withAllCSVFile" =>
					val reportableEvents: ReportableEvents = new ReportableEvents(Some(OPTION_UPLOAD_SPREEDSHEET))
					val fileType: CheckFileType = new CheckFileType(Some(OPTION_CSV))
					val findList = Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
					val addList = Map("ReportableEvents" -> Json.toJson(reportableEvents), "check-file-type" -> Json.toJson(fileType), "check-csv-files" -> Json.toJson(csvFilesCallbackList))
					val data = commonAllDataMap.filterKeys(findList.contains(_)) ++ addList
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
				case "withAllODSFile" =>
					val reportableEvents: ReportableEvents = new ReportableEvents(Some(OPTION_UPLOAD_SPREEDSHEET))
					val fileType: CheckFileType = new CheckFileType(Some(OPTION_ODS))
					val findList = Seq("scheme-organiser", "group-scheme-controller", "group-scheme-companies", "trustees", "ErsMetaData")
					val addList = Map("ReportableEvents" -> Json.toJson(reportableEvents), "check-file-type" -> Json.toJson(fileType), "check-csv-files" -> Json.toJson(csvFilesCallbackList), "file-name" -> Json.toJson(fileNameODS))
					val data = commonAllDataMap.filterKeys(findList.contains(_)) ++ addList
					val cm: CacheMap = new CacheMap("id1", data)
					Future.successful(cm)
			}
		}
	}

	lazy val ersConnector: ErsConnector = new ErsConnector(mockHttp, mockErsUtil, mockAppConfig) {
		override lazy val metrics: Metrics = mockMetrics
		override lazy val ersUrl = "ers-returns"
		override lazy val validatorUrl = "ers-file-validator"
		override def connectToEtmpSapRequest(schemeRef: String)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[String] = Future("1234567890")
	}


	def buildFakeSummaryDeclarationController(fetchMapVal: String = "e"): SummaryDeclarationController =
		new SummaryDeclarationController(messagesApi,
																		 mockAuthConnector,
																		 ersConnector,
																		 mockCountryCodes,
																		 new TestErsUtil(fetchMapVal),
																		 mockAppConfig
		) {
			when(mockHttp.POST[ValidatorData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
			(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
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
      contentAsString(await(controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (Nil Return) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("withAllNillReturn")
      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (CSV File Upload) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("withAllCSVFile")
      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (ODS File Upload) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("withAllODSFile")
      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements in the cache (ODS)" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("odsFile")
      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }

  "Calling SummaryDeclarationController.showSummaryDeclarationPage (GET) with authentication and required elements (no group scheme info) in the cache" should {
    "show the scheme organiser page" in {
      val controllerUnderTest = buildFakeSummaryDeclarationController("noGroupSchemeInfo")
      val result = controllerUnderTest.showSummaryDeclarationPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }
}
