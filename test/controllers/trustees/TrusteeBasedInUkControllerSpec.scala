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

import models.{RequestObject, RsFormMappings, TrusteeBasedInUk}
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
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status, stubBodyParser}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, trustee_based_in_uk}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeBasedInUkControllerSpec  extends AnyWordSpecLike
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
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)


  val testController = new TrusteeBasedInUkController(
    mockMCC,
    mockErsConnector,
    app.injector.instanceOf[global_error],
    testAuthAction,
    mockTrusteeService,
    mockCountryCodes,
    mockErsUtil,
    mockAppConfig,
    app.injector.instanceOf[trustee_based_in_uk]
  )

  "calling showQuestionPage" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

    "show the empty trustee based in UK question page when there is nothing to prefill" in {
      when(mockErsUtil.fetchPartFromTrusteeDetailsList[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(None))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_based.title"))
      contentAsString(result) should include(testMessages("ers_trustee_based.uk"))
      contentAsString(result) should include(testMessages("ers_trustee_based.overseas"))
    }

    "show the prefilled trustee based in UK question page when there is data to prefill" in {
      when(mockErsUtil.fetchPartFromTrusteeDetailsList[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(Some(TrusteeBasedInUk(true))))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_based.title"))
      contentAsString(result) should include(testMessages("ers_trustee_based.uk"))
      contentAsString(result) should include("value=\"0\"")
      contentAsString(result) should include("checked")
    }

    "show the global error page if an exception occurs while retrieving cached data" in {
      when(mockErsUtil.fetchPartFromTrusteeDetailsList[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.failed(new RuntimeException("Failure scenario")))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "calling handleQuestionSubmit" should {
    "show the trustee based in UK form page with errors if the form is incorrectly filled" in {
      when(mockErsUtil.getPageElement(any(), any(), any(), any())(any())).thenReturn("")
      val trusteeBasedData = Map("bool" -> "")
      val form = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.questionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include(testMessages("ers_trustee_based.title"))
      contentAsString(result) should include(testMessages("error.required"))
    }
  }

  "successfully bind the form and go to the trustee address UK page if true" in {
    val emptyCacheMap = CacheMap("", Map("" -> Json.obj()))
    when(mockErsUtil.cache[TrusteeBasedInUk](any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCacheMap))
    when(mockErsUtil.fetch[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(TrusteeBasedInUk(true)))
    when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

    val trusteeBasedData = Map("basedInUk" -> "0")
    val form = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
    val result = testController.questionSubmit(1).apply(authRequest)

    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result).get shouldBe routes.TrusteeAddressUkController.questionPage().url
  }

  "successfully bind the form and go to the trustee address overseas page if false" in {
    val emptyCacheMap = CacheMap("", Map("" -> Json.obj()))
    when(mockErsUtil.cache[TrusteeBasedInUk](any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCacheMap))
    when(mockErsUtil.fetch[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(TrusteeBasedInUk(false)))
    when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

    val trusteeBasedData = Map("basedInUk" -> "1")
    val form = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
    val result = testController.questionSubmit(1).apply(authRequest)

    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result).get shouldBe routes.TrusteeAddressOverseasController.questionPage().url
  }


  "calling editQuestion" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

    "be the same as showQuestion for a specific index" in {
      when(mockErsUtil.fetchPartFromTrusteeDetailsList[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(Some(TrusteeBasedInUk(false))))

      val result = testController.editQuestion(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_trustee_based.title"))
    }
  }

  "calling editQuestionSubmit" should {
    setAuthMocks()
    when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

    "successfully bind the form and go to the edit version of the trustee address UK page with the index preserved if the answer is true" in {
      val emptyCacheMap = CacheMap("", Map("" -> Json.obj()))
      when(mockErsUtil.cache[TrusteeBasedInUk](any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCacheMap))
      when(mockErsUtil.fetch[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(TrusteeBasedInUk(true)))
      when(mockErsUtil.fetchTrusteesOptionally(any())(any(), any())).thenReturn(Future.successful(Fixtures.exampleTrustees))
      when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

      val trusteeBasedData = Map("basedInUk" -> "0")
      val form = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.editQuestionSubmit(0).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.TrusteeAddressUkController.editQuestion(0).url
    }

    "successfully bind the form and go to the edit version of the trustee address overseas page with the index preserved if the answer is false" in {
      val emptyCacheMap = CacheMap("", Map("" -> Json.obj()))
      when(mockErsUtil.cache[TrusteeBasedInUk](any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCacheMap))
      when(mockErsUtil.fetch[TrusteeBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(TrusteeBasedInUk(false)))
      when(mockErsUtil.fetchTrusteesOptionally(any())(any(), any())).thenReturn(Future.successful(Fixtures.exampleTrustees))
      when(mockTrusteeService.updateTrusteeCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

      val trusteeBasedData = Map("basedInUk" -> "1")
      val form = RsFormMappings.trusteeBasedInUkForm().bind(trusteeBasedData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.editQuestionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.TrusteeAddressOverseasController.editQuestion(1).url
    }
  }
}
