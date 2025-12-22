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

package controllers.trustees

import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.ersRequestObject
import utils._
import views.html.{global_error, trustee_summary}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class TrusteeSummaryControllerSpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
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

	val tenThousand: Int = 10000
  val failure: Future[Nothing] = Future.failed(new Exception)
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val trusteeSummaryView: trustee_summary = app.injector.instanceOf[trustee_summary]

  val firstTrustee: TrusteeDetails = TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None, true)
  val secondTrustee: TrusteeDetails = TrusteeDetails("Second Trustee", "34 Some Road", None, None, None, Some("UK"), None, true)
  val thirdTrustee: TrusteeDetails = TrusteeDetails("Third Trustee", "60 Window Close", None, None, None, Some("UK"), None, true)

  val trusteeList: List[TrusteeDetails] = List(
    firstTrustee,
    secondTrustee,
    thirdTrustee
  )

  override def beforeEach() = {
    reset(mockErsUtil, mockSessionService)
    when(mockErsUtil.trusteeLocationMessage(any())).thenReturn("someLocation")
    when(mockErsUtil.buildAddressSummary(any())).thenReturn("addressSummary")
  }

  "calling Delete Trustee" should {
    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService, mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.deleteTrustee(tenThousand).apply(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value should include("sign-in")
    }

    "delete trustee for given index and redirect to trustee summary page" in {
      setAuthMocks()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      when(mockTrusteeService.deleteTrustee(any())(any())).thenReturn(Future.successful(true))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.deleteTrustee(1)(authRequest)

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustees")

      verify(mockTrusteeService, times(1))
        .deleteTrustee(meq(1))(any())
    }

    "return INTERNAL_SERVER_ERROR id delete returned false" in {
      setAuthMocks()
      when(mockTrusteeService.deleteTrustee(any())(any())).thenReturn(Future.successful(false))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.deleteTrustee(tenThousand).apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "calling trustee summary page" should {
    lazy val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000",Instant.now, "1", "2016", "EMI", "EMI")
    lazy val rsc: ErsMetaData = ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.trusteeSummaryPage().apply(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.trusteeSummaryPage().apply(Fixtures.buildFakeRequestWithSessionIdOTHER("GET"))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("govuk-heading-xl").text shouldBe Messages("ers_trustee_summary.title")


      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching trustee details list fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(failure)

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      contentAsString(controllerUnderTest.showTrusteeSummaryPage()(authRequest)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage()(testFakeRequest, testMessages))
      )
    }

    "direct to ers errors page if fetching request object fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(failure)

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      contentAsString(controllerUnderTest.showTrusteeSummaryPage()(authRequest)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage()(testFakeRequest, testMessages))
      )
    }

    "display trustee summary page pre-filled" in {
      setAuthMocks()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      when(mockSessionService.fetch[ErsMetaData](refEq(ERS_META_DATA))(any(), any())).thenReturn(Future.successful(rsc))
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[TrusteeDetailsList](refEq(TRUSTEES_CACHE))(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.showTrusteeSummaryPage()(authRequest)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("govuk-heading-xl").text shouldBe Messages("ers_trustee_summary.title")

      status(result) shouldBe Status.OK
    }

    "redirect to TrusteeNameController.questionPage if no trustees in list" in {
      setAuthMocks()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetch[TrusteeDetailsList](refEq(TRUSTEES_CACHE))(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(List.empty)))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(List.empty)))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.showTrusteeSummaryPage()(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustee-name")
    }

    "continue button gives a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.trusteeSummaryContinue().apply(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain(("Location" -> "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return&origin=ers-returns-frontend"))
    }

    "continue button give a status BadRequest on POST if user is authenticated and form data missing" in {
      setAuthMocks()
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService,  mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.trusteeSummaryContinue().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      status(result) shouldBe Status.BAD_REQUEST

      contentAsString(result) should include(testMessages("ers_trustee.add.err"))
      contentAsString(result) should include(testMessages("ers_trustee_summary.title"))
    }

    "continue button should redirect on POST if user is authenticated and addTrustee = true" in {
      setAuthMocks()
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))

      val addTrustee = Map("addTrustee" -> "0")
      val form = RsFormMappings.addTrusteeForm().bind(addTrustee)

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService, mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.trusteeSummaryContinue().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET").withFormUrlEncodedBody(form.data.toSeq: _*))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustee-name")
    }

    "continue button should redirect on POST if user is authenticated and addTrustee = false" in {
      setAuthMocks()
      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(trusteeList)))

      val addTrustee = Map("addTrustee" -> "1")
      val form = RsFormMappings.addTrusteeForm().bind(addTrustee)

      val controllerUnderTest = new TrusteeSummaryController(mockMCC, mockErsConnector, mockTrusteeService, mockSessionService, globalErrorView, trusteeSummaryView, testAuthAction)

      val result = controllerUnderTest.trusteeSummaryContinue().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET").withFormUrlEncodedBody(form.data.toSeq: _*))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/alterations-or-a-variation")
    }
  }
}
