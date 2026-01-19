/*
 * Copyright 2026 HM Revenue & Customs
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
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, reportable_events}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ReportableEventsControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ERSFakeApplicationConfig
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with ScalaFutures {

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
  implicit lazy val mat: Materializer = app.materializer
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val reportableEventsView: reportable_events  = app.injector.instanceOf[reportable_events]
  val TEST_OPTION_NIL_RETURN = "2"

  "calling Reportable Events Page" should {

    def buildFakeReportableEventsController(
      ersMetaDataRes: Boolean = true,
      ersMetaDataCachedOk: Boolean = true,
      sapRequestRes: Boolean = true,
      schemeOrganiserDetailsRes: Boolean = true,
      schemeOrganiserDataCached: Boolean = false,
      reportableEventsRes: Boolean = true
    ): ReportableEventsController = new ReportableEventsController(
      mockMCC,
      mockErsConnector,
      mockSessionService,
      globalErrorView,
      reportableEventsView,
      testAuthAction
    ) {
      val schemeInfo: SchemeInfo   = SchemeInfo("XA1100000000000",Instant.now, "1", "2016", "CSOP 2015/16", "CSOP")
      val ersMetaData: ErsMetaData = ErsMetaData(schemeInfo, "300.300.300.300", None, "", None, None)

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(mockErsConnector.connectToEtmpSapRequest(anyString())(any(), any()))
        .thenReturn(
          if (sapRequestRes)
            Future.successful(Right("1234567890"))
          else
            Future.successful(Left(new RuntimeException("ETMP call failed")))
        )


      when(mockSessionService.fetch[ReportableEvents](refEq(REPORTABLE_EVENTS))(any(), any()))
        .thenReturn(
          if (reportableEventsRes) Future.successful(ReportableEvents(Some(TEST_OPTION_NIL_RETURN)))
          else Future.failed(new NoSuchElementException)
        )

      when(mockSessionService.fetch[ErsMetaData](refEq(mockErsUtil.ERS_METADATA))(any(), any()))
        .thenReturn(if (ersMetaDataRes) Future.successful(ersMetaData) else Future.failed(new NoSuchElementException))

			when(mockSessionService.cache(refEq(mockErsUtil.ERS_METADATA), any())(any(), any()))
				.thenReturn(if (ersMetaDataCachedOk) Future.successful(null) else Future.failed(new Exception))

      when(
        mockSessionService.fetch[SchemeOrganiserDetails](refEq(mockErsUtil.SCHEME_ORGANISER_CACHE))(any(), any())
      )
        .thenReturn(
          if (schemeOrganiserDetailsRes) {
            if (schemeOrganiserDataCached) {
              Future.successful(
                SchemeOrganiserDetails("Name", Fixtures.companyName, None, None, None, None, None, None, None)
              )
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
      val result              = controllerUnderTest.reportableEventsPage().apply(FakeRequest("GET", ""))
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
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.updateErsMetaData(ersRequestObject)(authRequest, hc)
      status(result.asInstanceOf[Future[Result]]) shouldBe 500
      contentAsString(result.asInstanceOf[Future[Result]]) shouldBe contentAsString(
        Future.successful(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "direct to ers errors page if saving ersMetaData throws exception" in {
      val controllerUnderTest = buildFakeReportableEventsController(ersMetaDataCachedOk = false)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.updateErsMetaData(ersRequestObject)(authRequest, hc).futureValue
      status(result.asInstanceOf[Future[Result]])          shouldBe 500
      contentAsString(result.asInstanceOf[Future[Result]]) shouldBe contentAsString(
        Future.successful(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "show blank reportable events page if fetching reportableEvents throws exception" in {
      val controllerUnderTest = buildFakeReportableEventsController(reportableEventsRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showReportableEventsPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=upload-spreadsheet-radio-button]").hasAttr("checked") shouldEqual false
      document.select("input[id=nil-return-radio-button]").hasAttr("checked")         shouldEqual false
    }

    "show reportable events page with NO selected" in {
      val controllerUnderTest = buildFakeReportableEventsController()
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showReportableEventsPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=upload-spreadsheet-radio-button]").hasAttr("checked") shouldEqual false
      document.select("input[id=nil-return-radio-button]").hasAttr("checked")         shouldEqual true
    }

  }

  "calling Reportable Events Selected Page" should {

    def buildFakeReportableEventsController(
      ersMetaDataRes: Boolean = true,
      ersMetaDataCachedOk: Boolean = true,
      sapRequestRes: Boolean = true,
      schemeOrganiserDetailsRes: Boolean = true,
      schemeOrganiserDataCached: Boolean = false,
      reportableEventsRes: Boolean = true
    ): ReportableEventsController = new ReportableEventsController(
      mockMCC,
      mockErsConnector,
      mockSessionService,
      globalErrorView,
      reportableEventsView,
      testAuthAction
    ) {
      val schemeInfo: SchemeInfo   = SchemeInfo("XA1100000000000",Instant.now, "1", "2016", "CSOP 2015/16", "CSOP")
      val ersMetaData: ErsMetaData = ErsMetaData(schemeInfo, "300.300.300.300", None, "", None, None)

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(mockErsConnector.connectToEtmpSapRequest(anyString())(any(), any()))
        .thenReturn(
          if (sapRequestRes)
            Future.successful(Right("1234567890"))
          else
            Future.successful(Left(new RuntimeException("ETMP call failed")))
        )

      when(mockSessionService.fetch[ReportableEvents](refEq(REPORTABLE_EVENTS))(any(), any()))
        .thenReturn(
          if (reportableEventsRes) Future.successful(ReportableEvents(Some(TEST_OPTION_NIL_RETURN)))
          else Future.failed(new NoSuchElementException)
        )

      when(mockSessionService.fetch[ErsMetaData](refEq(mockErsUtil.ERS_METADATA))(any(), any()))
        .thenReturn(if (ersMetaDataRes) Future.successful(ersMetaData) else Future.failed(new NoSuchElementException))

      when(mockSessionService.cache(refEq(mockErsUtil.REPORTABLE_EVENTS), any())(any(), any()))
				.thenReturn(if (ersMetaDataCachedOk) Future.successful(null) else Future.failed(new Exception))

      when(mockSessionService.fetch[SchemeOrganiserDetails](refEq(mockErsUtil.SCHEME_ORGANISER_CACHE))(any(), any()))
        .thenReturn(
          if (schemeOrganiserDetailsRes) {
            if (schemeOrganiserDataCached) {
              Future.successful(
                SchemeOrganiserDetails("Name", Fixtures.companyName, None, None, None, None, None, None, None)
              )
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
      val result =
        controllerUnderTest.reportableEventsSelected().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "if nothing selected give a status of OK and show the reportable events page displaying form errors" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val reportableEventsData = Map("" -> "")
      val form = _root_.models.RsFormMappings.chooseForm().bind(reportableEventsData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }

    "give a redirect status to the Check File Type Page if YES selected for Reportable events" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val form = "isNilReturn" -> mockErsUtil.OPTION_UPLOAD_SPREEDSHEET
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(authRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckFileTypeController.checkFileTypePage().toString
    }

    "give a redirect status to the Scheme Organiser Page if NO selected for Reportable events" in {
      val controllerUnderTest: ReportableEventsController = buildFakeReportableEventsController()
      val form = "isNilReturn" -> mockErsUtil.OPTION_NIL_RETURN
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(authRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header
        .headers("Location") shouldBe controllers.schemeOrganiser.routes.SchemeOrganiserBasedInUkController.questionPage().toString
    }

    "direct to ers errors page if fetching reportableEvents throws exception" in {
      val controllerUnderTest: ReportableEventsController =
        buildFakeReportableEventsController(ersMetaDataCachedOk = false)
      val form = "isNilReturn" -> mockErsUtil.OPTION_NIL_RETURN
      val req = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form)
      val authRequest = buildRequestWithAuth(req)

      val result = controllerUnderTest.showReportableEventsSelected(ersRequestObject)(authRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeReportableEventsController().getGlobalErrorPage(req, testMessages))
      )
    }

  }

}
