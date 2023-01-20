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

package controllers

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
import views.html.{global_error, trustee_details, trustee_summary}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeControllerSpec extends AnyWordSpecLike
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
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)

	val tenThousand: Int = 10000
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val trusteeDetailsView: trustee_details = app.injector.instanceOf[trustee_details]
  val trusteeSummaryView: trustee_summary = app.injector.instanceOf[trustee_summary]

  "calling Trustee Details Page" should {

    val trusteeList = List(TrusteeDetails("Name", "1 The Street", None, None, None, Some("UK"), None))
		val groupScheme = GroupSchemeInfo(Some(mockErsUtil.OPTION_NO), Some(""))
    val failure: Future[Nothing] = Future.failed(new Exception)

    def buildFakeTrusteePageController(groupSchemeActivityRes: Future[GroupSchemeInfo] = Future.successful(groupScheme),
                                       trusteeDetailsRes: Future[TrusteeDetailsList] = Future.successful(TrusteeDetailsList(trusteeList)),
                                       cacheRes: Future[CacheMap] = Future.successful(mock[CacheMap])
																			): TrusteeController = new TrusteeController(mockMCC, mockAuthConnector, mockErsConnector, mockCountryCodes,
      mockErsUtil, mockAppConfig, globalErrorView, trusteeDetailsView, trusteeSummaryView, testAuthAction) {

			when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

			when(mockErsUtil.fetch[GroupSchemeInfo](matches(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any())(any(), any())
			) thenReturn groupSchemeActivityRes

      when(mockErsUtil.fetch[TrusteeDetailsList](matches(mockErsUtil.TRUSTEES_CACHE), any())(any(), any())
			) thenReturn trusteeDetailsRes

      when(mockErsUtil.cache(matches(mockErsUtil.TRUSTEES_CACHE), any(), any())(any(), any(), any())
      ) thenReturn cacheRes
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteePageController()
      val result = controllerUnderTest.trusteeDetailsPage(tenThousand).apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeTrusteePageController()
      val result = controllerUnderTest.trusteeDetailsPage(tenThousand).apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching groupSchemeActivity throws exception" in {
      val controllerUnderTest = buildFakeTrusteePageController(groupSchemeActivityRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showTrusteeDetailsPage(ersRequestObject, tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "show alterations trustee details page with no data pre-filled" in {
      val controllerUnderTest = buildFakeTrusteePageController()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showTrusteeDetailsPage(ersRequestObject, tenThousand)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "give a redirect status (to company authentication frontend) on POST if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteePageController()
      val result = controllerUnderTest.trusteeDetailsSubmit(tenThousand) apply FakeRequest("GET", "")
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on POST if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeTrusteePageController()
      val result = controllerUnderTest.trusteeDetailsSubmit(tenThousand) apply Fixtures.buildFakeRequestWithSessionIdCSOP("GET")
      status(result) shouldBe Status.OK
    }

    "give a OK status and stay on the same page if form errors" in {
      val controllerUnderTest = buildFakeTrusteePageController()
      val trusteeData = Map("" -> "")
      val form = RsFormMappings.trusteeDetailsForm.bind(trusteeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showTrusteeDetailsSubmit(ersRequestObject, tenThousand)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "if form errors and if fetching groupSchemeActivity fails direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteePageController(groupSchemeActivityRes = failure)
      val trusteeData = Map("" -> "")
      val form = RsFormMappings.trusteeDetailsForm.bind(trusteeData)
      val req = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(req)

      contentAsString(controllerUnderTest.showTrusteeDetailsSubmit(ersRequestObject, tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(req, testMessages)))
    }

    "if no form errors with new trustee (index tenThousand) and fetch trustee details success" in {
      val controllerUnderTest = buildFakeTrusteePageController()
      val trusteeData = Map("name" -> "Name",
				"addressLine1" -> "1 The Street",
				"addressLine2" -> "",
				"addressLine3" -> "",
				"addressLine4" -> "",
				"country" -> "UK",
				"postcode" -> ""
			)
      val form = RsFormMappings.trusteeDetailsForm.bind(trusteeData)
      val request = Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showTrusteeDetailsSubmit(ersRequestObject, tenThousand)(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.TrusteeController.trusteeSummaryPage().toString
    }

    "if no form errors with new trustee (index 10000) and fetch trustee details fails" in {
      val controllerUnderTest = buildFakeTrusteePageController(trusteeDetailsRes = failure)
      val trusteeData = Map("name" -> "Name",
				"addressLine1" -> "1 The Street",
				"addressLine2" -> "",
				"addressLine3" -> "",
				"addressLine4" -> "",
				"country" -> "UK",
				"postcode" -> ""
			)
      val form = RsFormMappings.trusteeDetailsForm.bind(trusteeData)
      val request = Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showTrusteeDetailsSubmit(ersRequestObject, tenThousand)(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.TrusteeController.trusteeSummaryPage().toString
    }

    "if no form errors and fetch trustee details success for not updating an existing trustee (index 1) " in {
      val controllerUnderTest = buildFakeTrusteePageController()
      val trusteeData = Map("name" -> "Name",
				"addressLine1" -> "1 The Street",
				"addressLine2" -> "",
				"addressLine3" -> "",
				"addressLine4" -> "",
				"country" -> "UK",
				"postcode" -> ""
			)
      val form = RsFormMappings.trusteeDetailsForm.bind(trusteeData)
      val request = Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showTrusteeDetailsSubmit(ersRequestObject, 1)(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.TrusteeController.trusteeSummaryPage().toString
    }

    "if no form errors and fetch trustee details success for updating a trustee (index 0) " in {
      val controllerUnderTest = buildFakeTrusteePageController()
      val trusteeData = Map("name" -> "Name",
				"addressLine1" -> "1 The Street",
				"addressLine2" -> "",
				"addressLine3" -> "",
				"addressLine4" -> "",
				"country" -> "UK",
				"postcode" -> ""
			)
      val form = RsFormMappings.trusteeDetailsForm.bind(trusteeData)
      val request = Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest = buildRequestWithAuth(request)

      val result = controllerUnderTest.showTrusteeDetailsSubmit(ersRequestObject, 0)(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.TrusteeController.trusteeSummaryPage().toString
    }

  }

  "calling Delete Trustee" should {

    val firstTrustee = TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None)
    val secondTrustee = TrusteeDetails("Second Trustee", "34 Some Road", None, None, None, Some("UK"), None)
    val thirdTrustee = TrusteeDetails("Third Trustee", "60 Window Close", None, None, None, Some("UK"), None)

    val trusteeList = List(
      firstTrustee,
      secondTrustee,
      thirdTrustee
    )

    val failure: Future[Nothing] = Future.failed(new Exception)

    def buildFakeTrusteeController(trusteeDetailsRes: Future[TrusteeDetailsList] = Future.successful(TrusteeDetailsList(trusteeList)),
																	 cacheRes: Future[CacheMap] = Future.successful(mock[CacheMap]),
																	 requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
																	): TrusteeController = new TrusteeController(mockMCC, mockAuthConnector, mockErsConnector, mockCountryCodes, mockErsUtil,
      mockAppConfig, globalErrorView, trusteeDetailsView, trusteeSummaryView, testAuthAction) {
			when(
        mockErsUtil.fetch[TrusteeDetailsList](refEq(mockErsUtil.TRUSTEES_CACHE), any())(any(), any())
      ) thenReturn trusteeDetailsRes

      when(
        mockErsUtil.cache(refEq(mockErsUtil.TRUSTEES_CACHE), any(), any())(any(), any(), any())
      ) thenReturn cacheRes

      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any())
      ) thenReturn requestObjectRes
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.deleteTrustee(tenThousand).apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.deleteTrustee(tenThousand).apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.SEE_OTHER
    }

    "throws exception if fetching trustee details direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteeController(trusteeDetailsRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showDeleteTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "throws exception if fetching request object direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteeController(requestObjectRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showDeleteTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "throws exception if cache fails direct to ers errors page" in {
      val controllerUnderTest = buildFakeTrusteeController(cacheRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showDeleteTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "delete trustee for given index and redirect to trustee summary page" in {

      val expected = List(firstTrustee, thirdTrustee)
      val cacheMap = CacheMap("_id", Map(mockErsUtil.TRUSTEES_CACHE -> Json.toJson(trusteeList)))

      val controllerUnderTest = buildFakeTrusteeController(cacheRes = Future.successful(cacheMap))
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showDeleteTrustee(1)(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER

      verify(mockErsUtil, times(1))
        .cache(meq("trustees"), meq(TrusteeDetailsList(expected)), meq(ersRequestObject.getSchemeReference))(any(), any(), any())
    }

  }

  "calling Edit Trustee" should {

    val trusteeList = List(TrusteeDetails("Name", "1 The Street", None, None, None, Some("UK"), None))

    val failure: Future[Nothing] = Future.failed(new Exception)

    def buildFakeTrusteeController(groupSchemeActivityRes: Future[GroupSchemeInfo] = Future(GroupSchemeInfo(Some(mockErsUtil.OPTION_NO), Some(""))),
																	 trusteeDetailsRes: Future[TrusteeDetailsList] = Future.successful(TrusteeDetailsList(trusteeList)),
																	 cacheRes: Future[CacheMap] = Future.successful(mock[CacheMap]),
																	 requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
                                  ): TrusteeController = new TrusteeController(mockMCC, mockAuthConnector, mockErsConnector, mockCountryCodes,
      mockErsUtil, mockAppConfig, globalErrorView, trusteeDetailsView, trusteeSummaryView, testAuthAction) {

			when(
        mockErsUtil.fetch[GroupSchemeInfo](refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), anyString())(any(), any())
      ) thenReturn groupSchemeActivityRes
      when(
        mockErsUtil.fetch[TrusteeDetailsList](refEq(mockErsUtil.TRUSTEES_CACHE), anyString())(any(), any())
      ) thenReturn trusteeDetailsRes
      when(
        mockErsUtil.cache(refEq(mockErsUtil.TRUSTEES_CACHE), anyString(), anyString())(any(), any(), any())
      ) thenReturn cacheRes

      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any())
      ) thenReturn requestObjectRes
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.editTrustee(tenThousand).apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.editTrustee(tenThousand).apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching group scheme activity fails" in {
      val controllerUnderTest = buildFakeTrusteeController(groupSchemeActivityRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showEditTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "direct to ers errors page if fetching trustee details list fails" in {
      val controllerUnderTest = buildFakeTrusteeController(trusteeDetailsRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showEditTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "direct to ers errors page if fetching request object fails" in {
      val controllerUnderTest = buildFakeTrusteeController(requestObjectRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showEditTrustee(tenThousand)(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "edit trustee for given index and display trustee summary page pre-filled" in {
      val controllerUnderTest = buildFakeTrusteeController()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showEditTrustee(0)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

    "traverse the trustee list and display trustee summary page" in {
      val controllerUnderTest = buildFakeTrusteeController()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showEditTrustee(10)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

  }

  "calling replace trustee" should {
    def controllerUnderTest: TrusteeController = new TrusteeController(mockMCC, mockAuthConnector, mockErsConnector, mockCountryCodes,
      mockErsUtil, mockAppConfig, globalErrorView, trusteeDetailsView, trusteeSummaryView, testAuthAction)

    "replace a trustee and keep the other trustees" when {

      "given an index that matches a trustee in the list" in {

        val index = 2

        val formData = TrusteeDetails("Replacement Trustee", "1 Some Place", None, None, None, None, None)
        val target = TrusteeDetails("Target Trustee", "3 Window Close", None, None, None, None, None)

        val trusteeDetailsList = List(
          TrusteeDetails("First Trustee", "20 Garden View", None, None, None, None, None),
          TrusteeDetails("Third Trustee", "72 Big Avenue", None, None, None, None, None),
          target,
          TrusteeDetails("Fourth Trustee", "21 Brick Lane", None, None, None, None, None)
        )

        val result = controllerUnderTest.replaceTrustee(trusteeDetailsList, index, formData)

        result should contain(formData)
        result shouldNot contain(target)
        result.length shouldBe 4
      }
    }

    "keep the existing list of trustees" when {

      "given an index that does not match any existing trustees" in {

        val index = 100

        val formData = TrusteeDetails("Replacement Trustee", "1 Some Place", None, None, None, None, None)
        val target = TrusteeDetails("Target Trustee", "3 Window Close", None, None, None, None, None)

        val trusteeDetailsList = List(
          TrusteeDetails("First Trustee", "20 Garden View", None, None, None, None, None),
          TrusteeDetails("Third Trustee", "72 Big Avenue", None, None, None, None, None),
          target,
          TrusteeDetails("Fourth Trustee", "21 Brick Lane", None, None, None, None, None)
        )

        val result = controllerUnderTest.replaceTrustee(trusteeDetailsList, index, formData)

        result shouldNot contain(formData)
        result should contain(target)
        result.length shouldBe 4
      }
    }

    "remove duplicate records" when {

      "duplicates are present" in {

        val index = 1

        val target = TrusteeDetails("Target Company", "3 Window Close", None, None, None, None, None)

        val trusteeDetailsList = List(
          target,
          target,
          target,
          target
        )

        val result = controllerUnderTest.replaceTrustee(trusteeDetailsList, index, target)

        result should contain(target)
        result.length shouldBe 1
      }
    }
  }

  "calling trustee summary page" should {

    val trusteeList = List(TrusteeDetails("Name", "1 The Street", None, None, None, Some("UK"), None))

    val failure: Future[Nothing] = Future.failed(new Exception)

    def buildFakeTrusteeController(trusteeDetailsRes: Future[TrusteeDetailsList] = Future.successful(TrusteeDetailsList(trusteeList)),
																	 cacheRes: Future[CacheMap] = Future.successful(mock[CacheMap]),
																	 requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
                                  ): TrusteeController = new TrusteeController(mockMCC, mockAuthConnector, mockErsConnector, mockCountryCodes, mockErsUtil,
      mockAppConfig, globalErrorView, trusteeDetailsView, trusteeSummaryView, testAuthAction) {

			when(
        mockErsUtil.fetch[TrusteeDetailsList](refEq(mockErsUtil.TRUSTEES_CACHE), anyString())(any(), any())
      ) thenReturn trusteeDetailsRes

      when(
        mockErsUtil.cache(refEq(mockErsUtil.TRUSTEES_CACHE), anyString(), anyString())(any(), any(), any())
      ) thenReturn cacheRes

      when(
        mockErsUtil.fetch[RequestObject](any())(any(), any(), any())
      ) thenReturn requestObjectRes
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.trusteeSummaryPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
			setAuthMocks()
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.trusteeSummaryPage().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching trustee details list fails" in {
      val controllerUnderTest = buildFakeTrusteeController(trusteeDetailsRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showTrusteeSummaryPage()(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "direct to ers errors page if fetching request object fails" in {
      val controllerUnderTest = buildFakeTrusteeController(requestObjectRes = failure)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      contentAsString(controllerUnderTest.showTrusteeSummaryPage()(authRequest, hc)) shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages)))
    }

    "display trustee summary page pre-filled" in {
      val controllerUnderTest = buildFakeTrusteeController()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showTrusteeSummaryPage()(authRequest, hc)
      status(result) shouldBe Status.OK
    }


    "continue button gives a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.trusteeSummaryContinue().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "continue button give a status OK on GET if user is authenticated" in {
      val controllerUnderTest = buildFakeTrusteeController()
      val result = controllerUnderTest.trusteeSummaryContinue().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to alterations activity page" in {
      val controllerUnderTest = buildFakeTrusteeController()
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.continueFromTrusteeSummaryPage()(authRequest, hc)
      status(result) shouldBe Status.SEE_OTHER
    }
  }
}
