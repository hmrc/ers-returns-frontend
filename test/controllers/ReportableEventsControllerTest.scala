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

import java.util.NoSuchElementException

import akka.stream.Materializer
import helpers.ErsTestHelper
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.Future

class ReportableEventsControllerTest extends UnitSpec with ERSFakeApplicationConfig with GuiceOneAppPerSuite with ErsTestHelper {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))
  implicit lazy val mat: Materializer = app.materializer
	val TEST_OPTION_NIL_RETURN = "2"
	val TEST_REPORTABLE_EVENTS = "ReportableEvents"

  "calling Reportable Events Page" should {

    def buildFakeReportableEventsController(ersMetaDataRes: Boolean = true,
																						ersMetaDataCachedOk: Boolean = true,
																						sapRequestRes: Boolean = true,
																						schemeOrganiserDetailsRes: Boolean = true,
																						schemeOrganiserDataCached: Boolean = false,
																						reportableEventsRes: Boolean = true
																					 ): ReportableEventsController = new ReportableEventsController(messagesApi, mockAuthConnector, mockErsConnector, mockErsUtil, mockAppConfig) {
      val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "CSOP 2015/16", "CSOP")
      val ersMetaData: ErsMetaData = ErsMetaData(schemeInfo, "300.300.300.300", None, "", None, None)

			when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(mockErsConnector.connectToEtmpSapRequest(anyString())(any(), any()))
				.thenReturn(if(sapRequestRes) Future.successful("1234567890") else Future.failed(new RuntimeException))

      when(mockErsUtil.fetch[ReportableEvents](refEq(TEST_REPORTABLE_EVENTS), anyString())(any(), any(), any()))
				.thenReturn(if(reportableEventsRes) Future.successful(ReportableEvents(Some(TEST_OPTION_NIL_RETURN))) else Future.failed(new NoSuchElementException))

			when(mockErsUtil.fetch[ErsMetaData](refEq(mockErsUtil.ersMetaData), anyString())(any(), any(), any()))
				.thenReturn(if (ersMetaDataRes) Future.successful(ersMetaData) else Future.failed(new NoSuchElementException))

			when(mockErsUtil.cache(refEq(mockErsUtil.ersMetaData), anyString(), anyString())(any(), any(), any()))
				.thenReturn(if (ersMetaDataCachedOk) Future.successful(null) else Future.failed(new Exception))

			when(mockErsUtil.fetch[SchemeOrganiserDetails](refEq(mockErsUtil.SCHEME_ORGANISER_CACHE), anyString())(any(), any(), any()))
				.thenReturn(
        if (schemeOrganiserDetailsRes) {
					if (schemeOrganiserDataCached) {
						Future.successful(SchemeOrganiserDetails("Name", Fixtures.companyName, None, None, None, None, None, None, None))
					} else {
						Future.successful(SchemeOrganiserDetails("", "", None, None, None, None, None, None, None))
					}
				} else {
					Future.failed(new NoSuchElementException)
				}
      )
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeReportableEventsController()
      val result = controllerUnderTest.reportableEventsPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeReportableEventsController()
      val result = controllerUnderTest.reportableEventsPage().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching ersMetaData throws exception" in {
      val controllerUnderTest = buildFakeReportableEventsController(ersMetaDataRes = false)
      val result = controllerUnderTest.updateErsMetaData(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
    }

    "direct to ers errors page if saving ersMetaData throws exception" in {
      val controllerUnderTest = buildFakeReportableEventsController(ersMetaDataCachedOk = false)
      val result = controllerUnderTest.updateErsMetaData(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
    }

    "show blank reportable events page if fetching reportableEvents throws exception" in {
      val controllerUnderTest = buildFakeReportableEventsController(reportableEventsRes = false)
      val result = await(controllerUnderTest.showReportableEventsPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc))
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=upload-spreadsheet-radio-button]").hasAttr("checked") shouldEqual false
      document.select("input[id=nil-return-radio-button]").hasAttr("checked") shouldEqual false
    }

    "show reportable events page with NO selected" in {
      val controllerUnderTest = buildFakeReportableEventsController()
      val result = await(controllerUnderTest.showReportableEventsPage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc))
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=upload-spreadsheet-radio-button]").hasAttr("checked") shouldEqual false
      document.select("input[id=nil-return-radio-button]").hasAttr("checked") shouldEqual true
    }

  }


  "calling Reportable Events Selected Page" should {

    def buildFakeReportableEventsController(ersMetaDataRes: Boolean = true,
																						ersMetaDataCachedOk: Boolean = true,
																						sapRequestRes: Boolean = true,
																						schemeOrganiserDetailsRes: Boolean = true,
																						schemeOrganiserDataCached: Boolean = false,
																						reportableEventsRes: Boolean = true
																					 ): ReportableEventsController = new ReportableEventsController(messagesApi, mockAuthConnector, mockErsConnector, mockErsUtil, mockAppConfig) {
      val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "CSOP 2015/16", "CSOP")
      val ersMetaData: ErsMetaData = ErsMetaData(schemeInfo, "300.300.300.300", None, "", None, None)

			when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(mockErsConnector.connectToEtmpSapRequest(anyString())(any(), any()))
				.thenReturn(if (sapRequestRes) Future.successful("1234567890") else Future.failed(new RuntimeException))

      when(mockErsUtil.fetch[ReportableEvents](refEq(TEST_REPORTABLE_EVENTS), any())(any(), any(), any()))
				.thenReturn(if (reportableEventsRes) Future.successful(ReportableEvents(Some(TEST_OPTION_NIL_RETURN))) else Future.failed(new NoSuchElementException))

      when(mockErsUtil.fetch[ErsMetaData](refEq(mockErsUtil.ersMetaData), any())(any(), any(), any()))
				.thenReturn(if (ersMetaDataRes) Future.successful(ersMetaData) else Future.failed(new NoSuchElementException))

      when(mockErsUtil.cache(refEq(mockErsUtil.reportableEvents), any(), any())(any(), any(), any()))
				.thenReturn(if (ersMetaDataCachedOk) Future.successful(null) else Future.failed(new Exception))

      when(mockErsUtil.fetch[SchemeOrganiserDetails](refEq(mockErsUtil.SCHEME_ORGANISER_CACHE), any())(any(), any(), any()))
				.thenReturn(
        if (schemeOrganiserDetailsRes) {
					if (schemeOrganiserDataCached) {
						Future.successful(SchemeOrganiserDetails("Name", Fixtures.companyName, None, None, None, None, None, None, None))
					} else {
						Future.successful(SchemeOrganiserDetails("", "", None, None, None, None, None, None, None))
					}
				} else {
					Future.failed(new NoSuchElementException)
				}
      )
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val result = controllerUnderTest.reportableEventsSelected().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val result = controllerUnderTest.reportableEventsSelected().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "if nothing selected give a status of OK and show the reportable events page displaying form errors" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val reportableEventsData = Map("" -> "")
      val form = _root_.models.RsFormMappings.chooseForm.bind(reportableEventsData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(Fixtures.buildFakeUser, request)
      status(result) shouldBe Status.OK
    }

    "give a redirect status to the Check File Type Page if YES selected for Reportable events" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val form = "isNilReturn" -> mockErsUtil.OPTION_UPLOAD_SPREEDSHEET
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form)
      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(Fixtures.buildFakeUser, request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckFileTypeController.checkFileTypePage().toString
    }

    "give a redirect status to the Scheme Organiser Page if NO selected for Reportable events" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val form = "isNilReturn" -> mockErsUtil.OPTION_NIL_RETURN
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form)
      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(Fixtures.buildFakeUser, request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.SchemeOrganiserController.schemeOrganiserPage().toString
    }

    "direct to ers errors page if fetching reportableEvents throws exception" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController(ersMetaDataCachedOk = false)
      val form = "isNilReturn" -> mockErsUtil.OPTION_NIL_RETURN
      val req = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form)
      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(Fixtures.buildFakeUser, req)
      status(result) shouldBe Status.OK
      contentAsString(result) should include(messages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(buildFakeReportableEventsController().getGlobalErrorPage(req, messages))
    }

  }

}
