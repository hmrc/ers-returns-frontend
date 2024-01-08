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

package controllers.trustees

import controllers.auth.RequestWithOptionalAuthContext
import forms.YesNoFormProvider
import models.{RequestObject, TrusteeDetailsList}
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
import utils.Fixtures.{ersRequestObject, exampleTrustees}
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, trustee_remove_yes_no}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeRemoveControllerSpec  extends AnyWordSpecLike
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

  val testController = new TrusteeRemoveController(
    mockMCC,
    testAuthAction,
    app.injector.instanceOf[trustee_remove_yes_no],
    app.injector.instanceOf[YesNoFormProvider],
    app.injector.instanceOf[global_error],
    mockTrusteeService,
    mockSessionService
  )(mockMCC.executionContext, mockAppConfig, mockErsUtil)

  "onPageLoad" should {
    val authRequest: RequestWithOptionalAuthContext[AnyContent] = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
    setAuthMocks()

    "show trusteeRemoveView" in {
      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(exampleTrustees))

      val result = testController.onPageLoad(1).apply(authRequest)

      status(result) shouldBe Status.OK

      contentAsString(result) should include(testMessages("ers_trustee_remove.title"))
      contentAsString(result) should include(testMessages("ers_trustee_remove.h1", exampleTrustees.trustees(1).name))
      contentAsString(result) should include(testMessages("ers.yes"))
      contentAsString(result) should include(testMessages("ers.no"))
    }

    "show redirect to TrusteeSummaryController.trusteeSummaryPage if requested index does not exist in mongo" in {
      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(List())))

      val result = testController.onPageLoad(10).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustees")
    }

    "show getGlobalErrorPage if fail returned from cache" in {
      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.failed(new Exception("fail")))

      val result = testController.onPageLoad(1).apply(authRequest)

      status(result) shouldBe Status.OK

      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "onSubmit" should {
    setAuthMocks()
    "redirect to TrusteeRemoveProblemController.onPageLoad if only one trustee in list" in {
      val authRequest: RequestWithOptionalAuthContext[AnyContent] =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(("value", "true")))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(TrusteeDetailsList(List(exampleTrustees.trustees.head))))

      val result = testController.onSubmit(0).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/remove-trustee/problem")
    }

    "redirect to TrusteeSummaryController.trusteeSummaryPage if more than one trustee in list" in {
      val authRequest: RequestWithOptionalAuthContext[AnyContent] =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(("value", "true")))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(exampleTrustees))
      when(mockTrusteeService.deleteTrustee(any())(any())).thenReturn(Future.successful(true))

      val result = testController.onSubmit(0).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustees")
    }

    "show getGlobalErrorPage if deleteTrustee returned false" in {
      val authRequest: RequestWithOptionalAuthContext[AnyContent] =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(("value", "true")))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(exampleTrustees))
      when(mockTrusteeService.deleteTrustee(any())(any())).thenReturn(Future.successful(false))

      val result = testController.onSubmit(0).apply(authRequest)

      status(result) shouldBe Status.OK

      contentAsString(result) shouldBe contentAsString(Future.successful(testController.getGlobalErrorPage))
    }

    "redirect to TrusteeSummaryController.trusteeSummaryPage if false submitted in form" in {
      val authRequest: RequestWithOptionalAuthContext[AnyContent] =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody(("value", "false")))

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(exampleTrustees))

      val result = testController.onSubmit(1).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER

      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/trustees")
    }

    "show trusteeRemoveView with errors if nothing selected" in {
      val authRequest: RequestWithOptionalAuthContext[AnyContent] =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("POST").withFormUrlEncodedBody())

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any())).thenReturn(Future.successful(exampleTrustees))

      val result = testController.onSubmit(1).apply(authRequest)

      status(result) shouldBe Status.BAD_REQUEST

      contentAsString(result) should include(testMessages("ers_trustee_remove.title"))
      contentAsString(result) should include(testMessages("ers_trustee_remove.error.required"))
      contentAsString(result) should include(testMessages("ers_trustee_remove.h1", exampleTrustees.trustees(1).name))
      contentAsString(result) should include(testMessages("ers.yes"))
      contentAsString(result) should include(testMessages("ers.no"))
    }
  }
}
