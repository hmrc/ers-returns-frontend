/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.trustees.TrusteeSummaryController
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.Fixtures.ersRequestObject
import utils.{ErsTestHelper, _}
import views.html.{global_error, trustee_summary}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeSummaryControllerSpec extends AnyWordSpecLike
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

	val tenThousand: Int = 10000
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val trusteeSummaryView: trustee_summary = app.injector.instanceOf[trustee_summary]

  "calling Delete Trustee" should {

    val firstTrustee = TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None, true)
    val secondTrustee = TrusteeDetails("Second Trustee", "34 Some Road", None, None, None, Some("UK"), None, true)
    val thirdTrustee = TrusteeDetails("Third Trustee", "60 Window Close", None, None, None, Some("UK"), None, true)

    val trusteeList = List(
      firstTrustee,
      secondTrustee,
      thirdTrustee
    )

    val failure: Future[Nothing] = Future.failed(new Exception)

    def buildFakeTrusteeController(trusteeDetailsRes: Future[TrusteeDetailsList] = Future.successful(TrusteeDetailsList(trusteeList)),
																	 cacheRes: Future[CacheMap] = Future.successful(mock[CacheMap]),
																	 requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
																	): TrusteeSummaryController = new TrusteeSummaryController(mockMCC, mockErsConnector, mockCountryCodes, mockErsUtil,
      mockAppConfig, globalErrorView, trusteeSummaryView, testAuthAction) {
			when(
        mockErsUtil.fetch[TrusteeDetailsList](refEq(mockErsUtil.TRUSTEES_CACHE), any())(any(), any())
      ) thenReturn trusteeDetailsRes

      when(
        mockErsUtil.cache(refEq(mockErsUtil.TRUSTEES_CACHE), any(), any())(any(), any())
      ) thenReturn cacheRes

      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any())
      ) thenReturn requestObjectRes
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result              = controllerUnderTest.deleteTrustee(tenThousand).apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result              =
        controllerUnderTest.deleteTrustee(tenThousand).apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      status(result) shouldBe Status.SEE_OTHER
    }

    "throws exception if fetching trustee details direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteeController(trusteeDetailsRes = failure)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showDeleteTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "throws exception if fetching request object direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteeController(requestObjectRes = failure)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showDeleteTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "throws exception if cache fails direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteeController(cacheRes = failure)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showDeleteTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "delete trustee for given index and redirect to trustee summary page" in {

      val expected = List(firstTrustee, thirdTrustee)
      val cacheMap = CacheMap("_id", Map(mockErsUtil.TRUSTEES_CACHE -> Json.toJson(trusteeList)))

      val controllerUnderTest = buildFakeTrusteeController(cacheRes = Future.successful(cacheMap))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      val result = controllerUnderTest.showDeleteTrustee(1)(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER

      verify(mockErsUtil, times(1))
        .cache(meq("trustees"), meq(TrusteeDetailsList(expected)), meq(ersRequestObject.getSchemeReference))(any(), any())
    }

  }

  "calling trustee summary page" should {

    val trusteeList = List(TrusteeDetails("Name", "1 The Street", None, None, None, Some("UK"), None, true))
    val failure: Future[Nothing] = Future.failed(new Exception("failure innit"))

    def buildFakeTrusteeController(trusteeDetailsRes: Future[TrusteeDetailsList] = Future.successful(TrusteeDetailsList(trusteeList)),
																	 cacheRes: Future[CacheMap] = Future.successful(mock[CacheMap]),
																	 requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
                                  ): TrusteeSummaryController = new TrusteeSummaryController(mockMCC, mockErsConnector, mockCountryCodes, mockErsUtil,
      mockAppConfig, globalErrorView, trusteeSummaryView, testAuthAction) {

      when(
        mockErsUtil.fetch[TrusteeDetailsList](refEq(mockErsUtil.TRUSTEES_CACHE), anyString())(any(), any())
      ) thenReturn trusteeDetailsRes

      when(
        mockErsUtil.cache(refEq(mockErsUtil.TRUSTEES_CACHE), anyString(), anyString())(any(), any())
      ) thenReturn cacheRes

      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any())
      ) thenReturn requestObjectRes

      when(
        mockErsUtil.fetchTrusteesOptionally(any())(any(), any())
      ) thenReturn trusteeDetailsRes
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result              = controllerUnderTest.trusteeSummaryPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    // These tests are going to the error page. It succeeds because the only check is that a 200 is returned and global error page is returning a 200 -.-
    // Raised https://jira.tools.tax.service.gov.uk/browse/DDCE-4841 to fix
    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result              = controllerUnderTest.trusteeSummaryPage().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching trustee details list fails" in {
      val controllerUnderTest = buildFakeTrusteeController(trusteeDetailsRes = failure)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showTrusteeSummaryPage()(authRequest, hc)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "direct to ers errors page if fetching request object fails" in {
      val controllerUnderTest = buildFakeTrusteeController(requestObjectRes = failure)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showTrusteeSummaryPage()(authRequest, hc)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    // These tests are going to the error page. It succeeds because the only check is that a 200 is returned and global error page is returning a 200 -.-
    // Raised https://jira.tools.tax.service.gov.uk/browse/DDCE-4841 to fix
    "display trustee summary page pre-filled" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      val result = controllerUnderTest.showTrusteeSummaryPage()(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "continue button gives a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result              = controllerUnderTest.trusteeSummaryContinue().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain(("Location" -> "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return&origin=ers-returns-frontend"))
    }

    // These tests are going to the error page. It succeeds because the only check is that a 200 is returned and global error page is returning a 200 -.-
    // Raised https://jira.tools.tax.service.gov.uk/browse/DDCE-4841 to fix
    "continue button give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result              = controllerUnderTest.trusteeSummaryContinue().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      status(result) shouldBe Status.OK
    }
  }
}

