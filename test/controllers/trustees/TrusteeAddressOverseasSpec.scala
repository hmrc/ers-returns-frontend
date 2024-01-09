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

import controllers.auth.RequestWithOptionalAuthContext
import models.{RequestObject, RsFormMappings, TrusteeAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status, stubBodyParser}
import utils.Fixtures.{ersRequestObject, trusteeAddressOverseas}
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, trustee_address_overseas}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeAddressOverseasSpec  extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite
  with ScalaFutures {

  implicit val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)

  val testController = new TrusteeAddressOverseasController(
    mockMCC,
    mockErsConnector,
    app.injector.instanceOf[global_error],
    testAuthAction,
    mockTrusteeService,
    mockSessionService,
    app.injector.instanceOf[trustee_address_overseas]
  )

  "calling showQuestionPage" should {
    implicit val authRequest: RequestWithOptionalAuthContext[AnyContent] = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "show the empty trustee address overseas question page when there is nothing to prefill" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeAddress](any())(any(), any())).thenReturn(Future.successful(None))
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeAddress](any())(any(), any())).thenReturn(Future.successful(None))
      val result = testController.questionPage(1).apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_address.title"))
      contentAsString(result) should include(testMessages("ers_trustee_address.line1"))
    }

    "show the prefilled trustee address overseas question page when there is data to prefill" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeAddress](any())(any(), any())).thenReturn(Future.successful(Some(trusteeAddressOverseas)))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_address.title"))
      contentAsString(result) should include(testMessages("ers_trustee_address.line1"))
      contentAsString(result) should include("Overseas line 1")
    }

    "show the global error page if an exception occurs while retrieving cached data" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeAddress](any())(any(), any())).thenReturn(Future.failed(new RuntimeException("Failure scenario")))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "nextPageRedirect" should {
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "redirect to TrusteeSummaryController.trusteeSummaryPage if edit true" in {

      val result = testController.nextPageRedirect(0, edit = true)

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustees")
    }

    "update trustee cache and redirect to TrusteeSummaryController.trusteeSummaryPage if edit false" in {
      when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()))

      val result = testController.nextPageRedirect(0, edit = false)

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustees")
    }
  }

  "calling questionSubmit" should {
    "show the trustee address overseas form page with errors if the form is incorrectly filled" in {
      val trusteeAddressOverseasData = Map("addressLine1" -> "")
      val form = RsFormMappings.trusteeAddressOverseasForm().bind(trusteeAddressOverseasData)
      implicit val authRequest: RequestWithOptionalAuthContext[AnyContent] = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.questionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include(testMessages("ers_trustee_address.title"))
      contentAsString(result) should include(testMessages("ers_trustee_details.err.summary.address_line1_required"))
    }

    "successfully bind the form and redirect to the trustee summary page" in {
      when(mockSessionService.cache[TrusteeAddress](any(), any())(any(), any())).thenReturn(Future.successful(("sessionId", "someId")))
      when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

      val trusteeAddressOverseasData = Map("addressLine1" -> "123 Fake Street")
      val form = RsFormMappings.trusteeAddressOverseasForm().bind(trusteeAddressOverseasData)
      implicit val authRequest: RequestWithOptionalAuthContext[AnyContent] = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.questionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.TrusteeSummaryController.trusteeSummaryPage().url
    }
  }

  "calling editQuestion" should {
    implicit val authRequest: RequestWithOptionalAuthContext[AnyContent] = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "be the same as showQuestion for a specific index" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeAddress](any())(any(), any())).thenReturn(Future.successful(Some(trusteeAddressOverseas)))

      val result = testController.editQuestion(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_address.title"))
      contentAsString(result) should include(testMessages("ers_trustee_address.line1"))
      contentAsString(result) should include("Overseas line 1")

    }
  }
}
