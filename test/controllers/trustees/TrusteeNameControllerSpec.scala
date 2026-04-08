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

package controllers.trustees

import models.{RequestObject, RsFormMappings, TrusteeName}
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
import play.api.mvc.{
  AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents
}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status, stubBodyParser}
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, trustee_name}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeNameControllerSpec
    extends AnyWordSpecLike
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

  val testController = new TrusteeNameController(
    mockMCC,
    mockErsConnector,
    app.injector.instanceOf[global_error],
    testAuthAction,
    mockTrusteeService,
    mockSessionService,
    app.injector.instanceOf[trustee_name]
  )

  "calling showQuestionPage" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "show the empty trustee name question page when there is nothing to prefill" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeName](any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = testController.questionPage(1).apply(authRequest)

      status(result)        shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_name.title"))
    }

    "show the prefilled trustee name question page when there is data to prefill" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeName](any())(any(), any()))
        .thenReturn(Future.successful(Some(TrusteeName("Test person"))))

      val result = testController.questionPage(1).apply(authRequest)

      status(result)        shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_name.title"))
      contentAsString(result) should include("Test person")

    }

    "show the global error page if an exception occurs while retrieving cached data" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeName](any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Failure scenario")))

      val result = testController.questionPage(1).apply(authRequest)

      status(result)        shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "calling handleQuestionSubmit" should {
    "show the trustee name form page with errors if the form is incorrectly filled" in {
      val trusteeBasedData     = Map("bool" -> "")
      val form                 = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
      implicit val authRequest = buildRequestWithAuth(
        Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )
      val result               = testController.questionSubmit(1).apply(authRequest)

      status(result)        shouldBe Status.BAD_REQUEST
      contentAsString(result) should include(testMessages("ers_trustee_name.title"))
      contentAsString(result) should include(testMessages("error.required"))
    }

    "successfully bind the form and go to the trustee based in UK page if the form is filled correctly" in {
      when(mockSessionService.cache[TrusteeName](any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
      when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

      val trusteeBasedData     = Map("name" -> "Test person")
      val form                 = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
      implicit val authRequest = buildRequestWithAuth(
        Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )
      val result               = testController.questionSubmit(1).apply(authRequest)

      status(result)               shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.TrusteeBasedInUkController.questionPage().url

    }
  }

  "calling editQuestion" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "be the same as showQuestion for a specific index" in {
      when(mockSessionService.fetchPartFromTrusteeDetailsList[TrusteeName](any())(any(), any()))
        .thenReturn(Future.successful(Some(TrusteeName("Test person"))))

      val result = testController.editQuestion(1).apply(authRequest)

      status(result)        shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_name.title"))
      contentAsString(result) should include("Test person")
    }
  }

  "calling editQuestionSubmit" should {
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "successfully bind the form and go to the edit version of the trustee based in UK page with the index preserved if the form is filled correctly" in {
      when(mockSessionService.cache[TrusteeName](any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
      when(mockSessionService.fetchTrusteesOptionally()(any(), any()))
        .thenReturn(Future.successful(Fixtures.exampleTrustees))
      when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

      val trusteeBasedData     = Map("name" -> "Test person")
      val form                 = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
      implicit val authRequest = buildRequestWithAuth(
        Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )
      val result               = testController.editQuestionSubmit(1).apply(authRequest)

      status(result)               shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.TrusteeBasedInUkController.editQuestion(1).url
    }
  }

}
