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

import controllers.auth.RequestWithOptionalAuthContext
import models._
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Fixtures.ersRequestObject
import utils._
import views.html.{confirmation, global_error}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ConfirmationPageControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ERSFakeApplicationConfig
    with ErsTestHelper
    with BeforeAndAfterEach
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

  implicit lazy val mat: Materializer = app.materializer
  val globalErrorView: global_error   = app.injector.instanceOf[global_error]
  val confirmationView: confirmation  = app.injector.instanceOf[confirmation]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockErsUtil)
    when(mockErsUtil.ERS_REQUEST_OBJECT).thenReturn("ErsRequestObject")
    when(mockErsUtil.ERS_METADATA).thenReturn("ErsMetaData")
    when(mockErsUtil.OPTION_NIL_RETURN).thenReturn("2")
    when(mockErsUtil.VALIDATED_SHEETS).thenReturn("validated-sheets")
  }

  "calling showConfirmationPage" should {

    val schemeInfo           = SchemeInfo("XA1100000000000", Instant.now, "1", "2016", "EMI", "EMI")
    val rsc                  = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
    val ersSummary           =
      ErsSummary("testbundle", "1", None, Instant.now, rsc, None, None, None, None, None, None, None, None)
    val ersSummaryNilReturn2 =
      ErsSummary("testbundle", "2", None, Instant.now, rsc, None, None, None, None, None, None, None, None)

    def buildFakeConfirmationPageController(
      isNilReturn: Boolean = false,
      bundleRes: Future[String] = Future.successful("Bundle12345"),
      allDataRes: Future[ErsSummary] = Future.successful(ersSummary),
      ersMetaRes: Future[ErsMetaData] = Future.successful(rsc),
      presubmission: Future[HttpResponse] = Future.successful(HttpResponse(OK, "")),
      requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
    ): ConfirmationPageController =
      new ConfirmationPageController(
        mockMCC,
        mockErsConnector,
        mockAuditEvents,
        mockSessionService,
        globalErrorView,
        confirmationView,
        testAuthAction
      ) {

        when(
          mockErsConnector.connectToEtmpSummarySubmit(anyString(), any[JsValue]())(any(), any())
        ) thenReturn bundleRes
        when(
          mockErsConnector.checkForPresubmission(any[SchemeInfo](), anyString())(any(), any())
        ) thenReturn presubmission

        when(mockSessionService.fetch[ErsMetaData](refEq("ErsMetaData"))(any(), any())) thenReturn ersMetaRes

        when(mockSessionService.getAllData(anyString(), any[ErsMetaData]())(any(), any(), any())) thenReturn Future
          .successful(
            if (isNilReturn) ersSummaryNilReturn2 else ersSummary
          )

        when(mockSessionService.fetch[String](refEq("validated-sheets"))(any(), any())) thenReturn Future
          .successful("")

        when(
          mockSessionService.fetch[RequestObject](refEq("ErsRequestObject"))(any(), any())
        ) thenReturn requestObjectRes

        override def saveAndSubmit(alldata: ErsSummary, all: ErsMetaData, bundle: String)(implicit
          request: RequestWithOptionalAuthContext[AnyContent],
          hc: HeaderCarrier
        ): Future[Result] = Future(Ok)
      }

    "give a redirect status (to company authentication frontend) if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest: ConfirmationPageController = buildFakeConfirmationPageController()
      val result                                          = controllerUnderTest.confirmationPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest: ConfirmationPageController = buildFakeConfirmationPageController()
      val result                                          = controllerUnderTest.confirmationPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "show user research banner for confirmation page" in {
      val result   = confirmationView(ersRequestObject, "8 April 2016, 4:50pm", "", "", "")(
        Fixtures.buildFakeRequestWithSessionId("GET"),
        testMessages,
        mockErsUtil,
        mockAppConfig
      )
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("hmrc-user-research-banner").isEmpty shouldBe true
    }

    "direct to ers errors page if bundle request throws exception" in {
      val controllerUnderTest: ConfirmationPageController =
        buildFakeConfirmationPageController(bundleRes = Future.failed(new RuntimeException))
      val authRequest                                     = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result                                          = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "direct to ers errors page if fetching all data throws exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController()
      when(mockSessionService.getAllData(anyString(), any[ErsMetaData]())(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result              = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "direct to ers errors page if fetching request object throws exception" in {
      val controllerUnderTest =
        buildFakeConfirmationPageController(requestObjectRes = Future.failed(new RuntimeException))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result              = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "return OK if there are no exceptions thrown and confirmation date time already exists" in {
      val mockedSession = mock[Session]
      val mockedRequest = mock[RequestHeader]

      when(
        mockedSession.get(anyString())
      ) thenReturn Some("")

      when(
        mockedRequest.session
      ) thenReturn mockedSession

      val controllerUnderTest = buildFakeConfirmationPageController(isNilReturn = true)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching metadata throws exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController(ersMetaRes = Future.failed(new RuntimeException))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result              = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "returns OK for NilReturn if there are no exceptions thrown" in {
      val request             = FakeRequest().withSession("screenSchemeInfo" -> "10 MAR 2016")
      val controllerUnderTest = buildFakeConfirmationPageController(isNilReturn = true)
      val result              = controllerUnderTest.showConfirmationPage()(buildRequestWithAuth(request), hc)
      status(result) shouldBe Status.OK
    }

    "returns OK for submission if there are no exceptions thrown" in {
      val controllerUnderTest = buildFakeConfirmationPageController()
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if check for presubmission returns status != OK" in {
      val controllerUnderTest =
        buildFakeConfirmationPageController(presubmission = Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result              = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "direct to ers errors page if check for presubmission fails" in {
      val controllerUnderTest = buildFakeConfirmationPageController(presubmission = Future.failed(new RuntimeException))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result              = controllerUnderTest.showConfirmationPage()(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "show the confirmation page without re-submitting if a bundleRef already exists" in {
      when(mockAppConfig.portalDomain).thenReturn("/")
      val controllerUnderTest = buildFakeConfirmationPageController()
      val authRequest         =
        buildRequestWithAuth(
          Fixtures
            .buildFakeRequestWithSessionId("GET")
            .withSession(
              ("bundleRef", "123456"),
              ("dateTimeSubmitted", "8 April 2016, 4:50pm")
            )
        )
      val result              = controllerUnderTest.showConfirmationPage()(authRequest, hc)

      status(result)        shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_confirmation.submitted"))
    }

  }

  "calling saveAndSubmit" should {
    val schemeInfo = SchemeInfo("XA1100000000000", Instant.now, "1", "2016", "EMI", "EMI")
    val rsc        = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
    val ersSummary =
      ErsSummary("testbundle", "1", None, Instant.now(), rsc, None, None, None, None, None, None, None, None)

    def buildFakeConfirmationPageController(
      saveMetadataRes: Boolean = true,
      saveMetadataResponse: Int = OK,
      submitReturnToBackendResponse: Int = OK
    ): ConfirmationPageController =
      new ConfirmationPageController(
        mockMCC,
        mockErsConnector,
        mockAuditEvents,
        mockSessionService,
        globalErrorView,
        confirmationView,
        testAuthAction
      ) {

        when(mockErsConnector.saveMetadata(any[ErsSummary]())(any(), any()))
          .thenReturn(
            if (saveMetadataRes) {
              Future.successful(HttpResponse(saveMetadataResponse, ""))
            } else {
              Future.failed(new RuntimeException)
            }
          )

        when(mockErsConnector.submitReturnToBackend(any[ErsSummary]())(any(), any())) thenReturn Future.successful(
          HttpResponse(submitReturnToBackendResponse, "")
        )
      }

    "returns OK for Submission if there are no exceptions thrown - submit to backend successful" in {
      val controllerUnderTest = buildFakeConfirmationPageController()
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result =
        controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "returns OK for Submission if there are no exceptions thrown - submit to backend fails" in {
      val controllerUnderTest =
        buildFakeConfirmationPageController(submitReturnToBackendResponse = INTERNAL_SERVER_ERROR)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result =
        controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "returns OK for Submission if there are no exceptions thrown - save meta data to backend fails" in {
      val controllerUnderTest = buildFakeConfirmationPageController(saveMetadataResponse = INTERNAL_SERVER_ERROR)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      val result =
        controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "displays the global error page for Submission if save meta data to backend throws an exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController(saveMetadataRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result              =
        controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(authRequest, hc)
      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }
  }

}
