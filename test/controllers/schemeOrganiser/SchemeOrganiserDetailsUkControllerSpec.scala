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

package controllers.schemeOrganiser

import models.{Company, RequestObject, RsFormMappings}
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
import play.api.routing.Router.empty.routes
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status, stubBodyParser}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, manual_company_details_uk}

import scala.concurrent.{ExecutionContext, Future}

class SchemeOrganiserDetailsUkControllerSpec extends AnyWordSpecLike
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


  val testController = new SchemeOrganiserDetailsUkController(
    mockMCC,
    mockAuthConnector,
    mockErsConnector,
    app.injector.instanceOf[global_error],
    testAuthAction,
    mockCountryCodes,
    mockErsUtil,
    mockAppConfig,
    app.injector.instanceOf[manual_company_details_uk]
  )
  "calling showQuestionPage" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

    "show the empty company name question page when there is nothing to prefill" in {
      when(mockErsUtil.fetchPartFromCompanyDetailsList[Company](any(), any())(any(), any())).thenReturn(Future.successful(None))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_manual_company_details_uk.title"))
    }

    "show the prefilled company name question page when there is data to prefill" in {
      when(mockErsUtil.fetchPartFromCompanyDetailsList[Company](any(), any())(any(), any())).thenReturn(Future.successful(Some(Company("Test Company", Some("AA123456"), Some("1234567890")))))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_manual_company_details_uk.title"))
      contentAsString(result) should include("Test Company")
      contentAsString(result) should include("AA123456")
      contentAsString(result) should include("1234567890")

    }

    "show the global error page if an exception occurs while retrieving cached data" in {
      when(mockErsUtil.fetchPartFromCompanyDetailsList[Company](any(), any())(any(), any())).thenReturn(Future.failed(new RuntimeException("Failure scenario")))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "calling handleQuestionSubmit" should {
    "show the company name form page with errors if the form is incorrectly filled" in {
      val companyData = Map("bool" -> "")
      val form = RsFormMappings.companyNameForm().bind( companyData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.questionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include(testMessages("ers_manual_company_details_uk.title"))
      contentAsString(result) should include(testMessages("error.required"))
    }

    "successfully bind the form and go to the company uk address page if the form is filled correctly" in {
      val emptyCacheMap = CacheMap("", Map("" -> Json.obj()))
      when(mockErsUtil.cache[Company](any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCacheMap))
      when(mockCompanyDetailsService.updateSubsidiaryCompanyCache(any())(any())).thenReturn(Future.successful(()), Future.successful(()))

      val companyData = Map("name" -> "Test company")
      val form = RsFormMappings.companyNameForm().bind(companyData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.questionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe controllers.schemeOrganiser.routes.SchemeOrganiserAddressUkController.questionPage().url

    }
  }
}
