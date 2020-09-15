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
import models._
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.{Request, Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils._
import views.html.confirmation

import scala.concurrent.Future

class ConfirmationPageControllerTest extends UnitSpec with ERSFakeApplicationConfig with ErsTestHelper with BeforeAndAfterEach with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer
  implicit lazy val messages: Messages = Messages(Lang("en"), app.injector.instanceOf[MessagesApi])
	lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

	override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockErsUtil)
		when(mockErsUtil.ersRequestObject).thenReturn("ErsRequestObject")
		when(mockErsUtil.ersMetaData).thenReturn("ErsMetaData")
		when(mockErsUtil.OPTION_NIL_RETURN).thenReturn("2")
		when(mockErsUtil.VALIDATED_SHEEETS).thenReturn("validated-sheets")
	}

  "calling ConformationPage" should {

    val schemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")
    val rsc = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
    val ersSummary = ErsSummary("testbundle", "1", None, DateTime.now, rsc, None, None, None, None, None, None, None, None)

    def buildFakeConfirmationPageController(
                                             isNilReturn: Boolean = false,
                                             bundleRes: Future[String] = Future.successful("Bundle12345"),
                                             allDataRes: Future[ErsSummary] = Future.successful(ersSummary),
                                             ersMetaRes: Future[ErsMetaData] = Future.successful(rsc),
                                             presubmission: Future[HttpResponse] = Future.successful(HttpResponse(OK)),
                                             requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
                                           ): ConfirmationPageController =
			new ConfirmationPageController(messagesApi, mockErsConnector, mockAuthConnector, mockAuditEvents, mockErsUtil, mockAppConfig) {

      when(mockErsConnector.connectToEtmpSummarySubmit(anyString(), any[JsValue]())(any(), any())) thenReturn bundleRes
      when(mockErsConnector.checkForPresubmission(any[SchemeInfo](), anyString())(any(), any())) thenReturn presubmission

      when(mockErsUtil.fetch[ErsMetaData](refEq("ErsMetaData"), anyString())(any(), any(), any())
      ) thenReturn ersMetaRes

      when(mockErsUtil.getAllData(anyString(), any[ErsMetaData]())(any(), any(), any())
      ) thenReturn (if (isNilReturn) allDataRes.copy(isNilReturn = "2") else ersSummary)

      when(mockErsUtil.fetch[String](refEq("validated-sheets"), anyString())(any(), any(), any())
      ) thenReturn Future.successful("")

      when(mockErsUtil.fetch[RequestObject](refEq("ErsRequestObject"))(any(), any(), any(), any())
      ) thenReturn requestObjectRes

      override def saveAndSubmit(alldata: ErsSummary, all: ErsMetaData, bundle: String)
																(implicit authContext: ERSAuthData,
																 request: Request[AnyRef],
																 hc: HeaderCarrier): Future[Result] = Future(Ok)
    }

    "give a redirect status (to company authentication frontend) if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest: ConfirmationPageController = buildFakeConfirmationPageController()
      val result = controllerUnderTest.confirmationPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest: ConfirmationPageController = buildFakeConfirmationPageController()
      val result = controllerUnderTest.confirmationPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK

    }

    "show user panel for confirmation page" in {
      val result = confirmation(ersRequestObject, "8 April 2016, 4:50pm", "", "", "")(Fixtures.buildFakeRequestWithSessionId("GET"), messages, mockErsUtil, mockAppConfig)
      contentAsString(result)  should include("Help improve digital services by joining the HMRC user panel (opens in new window)")
      contentAsString(result)  should include("No thanks")

    }

    "direct to ers errors page if bundle request throws exception" in {
      val controllerUnderTest: ConfirmationPageController = buildFakeConfirmationPageController(bundleRes = Future.failed(new RuntimeException))
      val result = await(controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

    "direct to ers errors page if fetching all data throws exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController()
			when(mockErsUtil.getAllData(anyString(), any[ErsMetaData]())(any(), any(), any())).thenReturn(Future.failed(new RuntimeException))
      val result = await(controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

    "direct to ers errors page if fetching request object throws exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController(requestObjectRes = Future.failed(new RuntimeException))
      val result = await(controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

    "return OK if there are no exceptions thrown and confirmation date time already exists" in {
      val mockedSession = mock[Session]
      val mockedRequest = mock[Request[AnyRef]]

      when(
        mockedSession.get(anyString())
      ) thenReturn Some("")

      when(
        mockedRequest.session
      ) thenReturn mockedSession

      val controllerUnderTest = buildFakeConfirmationPageController(isNilReturn = true)
      val result = controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching metadata throws exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController(ersMetaRes = Future.failed(new RuntimeException))
      val result = await(controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

    "returns OK for NilReturn if there are no exceptions thrown" in {
      val request = FakeRequest().withSession("screenSchemeInfo" -> "10 MAR 2016")
      val controllerUnderTest = buildFakeConfirmationPageController(isNilReturn = true)
      val result = controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, request, hc)
      status(result) shouldBe Status.OK
    }

    "returns OK for submission if there are no exceptions thrown" in {
      val controllerUnderTest = buildFakeConfirmationPageController()
      val result = controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if check for presubmission returns status != OK" in {
      val controllerUnderTest = buildFakeConfirmationPageController(presubmission = Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
      val result = await(controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

    "direct to ers errors page if check for presubmission fails" in {
      val controllerUnderTest = buildFakeConfirmationPageController(presubmission = Future.failed(new RuntimeException))
      val result = await(controllerUnderTest.showConfirmationPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

  }

  "calling saveAndSubmit" should {
    val schemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")
    val rsc = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
    val ersSummary = ErsSummary("testbundle", "1", None, DateTime.now, rsc, None, None, None, None, None, None, None, None)

    def buildFakeConfirmationPageController(saveMetadataRes: Boolean = true,
																						saveMetadataResponse: Int = OK,
																						submitReturnToBackendResponse: Int = OK
																					 ): ConfirmationPageController =
			new ConfirmationPageController(messagesApi, mockErsConnector, mockAuthConnector, mockAuditEvents, mockErsUtil, mockAppConfig) {

      when(mockErsConnector.saveMetadata(any[ErsSummary]())(any(), any()))
				.thenReturn(
        if (saveMetadataRes) {
					Future.successful(HttpResponse(saveMetadataResponse))
				} else {
					Future.failed(new RuntimeException)
				}
      )

      when(mockErsConnector.submitReturnToBackend(any[ErsSummary]())(any(), any())).thenReturn(HttpResponse(submitReturnToBackendResponse))
    }

    "returns OK for Submission if there are no exceptions thrown - submit to backend successful" in {
      val controllerUnderTest = buildFakeConfirmationPageController()
      val result = controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }

    "returns OK for Submission if there are no exceptions thrown - submit to backend fails" in {
      val controllerUnderTest = buildFakeConfirmationPageController(submitReturnToBackendResponse = INTERNAL_SERVER_ERROR)
      val result = controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }

    "returns OK for Submission if there are no exceptions thrown - save meta data to backend fails" in {
      val controllerUnderTest = buildFakeConfirmationPageController(saveMetadataResponse = INTERNAL_SERVER_ERROR)
      val result = await(controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }

    "displays the global error page for Submission if save meta data to backend throws an exception" in {
      val controllerUnderTest = buildFakeConfirmationPageController(saveMetadataRes = false)
      val result = await(controllerUnderTest.saveAndSubmit(ersSummary, ersSummary.metaData, ersSummary.bundleRef)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
      contentAsString(result) should include(messages("ers.global_errors.message"))
    }
  }
}
