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

package controllers.subsidiaries

import models.{Company, RequestObject, RsFormMappings}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, when}
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
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, manual_company_details_uk}

import scala.concurrent.{ExecutionContext, Future}

class SubsidiaryDetailsUKControllerSpec extends AnyWordSpecLike
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


    val testController = new SubsidiaryDetailsUkController(
      mockMCC,
      mockAuthConnector,
      mockErsConnector,
      app.injector.instanceOf[global_error],
      testAuthAction,
      mockCountryCodes,
      mockErsUtil,
      mockSessionService,
      mockAppConfig,
      app.injector.instanceOf[manual_company_details_uk]
    )
    "calling showQuestionPage" should {
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      setAuthMocks()
      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

      "show the empty company name question page when there is nothing to prefill" in {
        when(mockSessionService.fetchPartFromCompanyDetailsList[Company](any())(any(), any())).thenReturn(Future.successful(None))

        val result = testController.questionPage(1).apply(authRequest)

        status(result) shouldBe Status.OK
        contentAsString(result) should include(testMessages("ers_manual_company_details_uk.title"))
      }

      "show the prefilled company name question page when there is data to prefill" in {
        when(mockSessionService.fetchPartFromCompanyDetailsList[Company](any())(any(), any())).thenReturn(Future.successful(Some(Company("Test Company", Some("AA123456"), Some("1234567890")))))

        val result = testController.questionPage(1).apply(authRequest)

        status(result) shouldBe Status.OK
        contentAsString(result) should include(testMessages("ers_manual_company_details_uk.title"))
        contentAsString(result) should include("Test Company")
        contentAsString(result) should include("AA123456")
        contentAsString(result) should include("1234567890")

      }

      "show the global error page if an exception occurs while retrieving cached data" in {
        when(mockSessionService.fetchPartFromCompanyDetailsList[Company](any())(any(), any())).thenReturn(Future.failed(new RuntimeException("Failure scenario")))

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
        when(mockSessionService.cache[Company](any(), any())(any(), any())).thenReturn(Future.successful(("","")))
        doNothing().when(mockCompanyDetailsService).updateSubsidiaryCompanyCache(any())(any())

        val companyData = Map("companyName" -> "Test company")

        val form = RsFormMappings.companyNameForm().bind(companyData)

        implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
        val result = testController.questionSubmit(1).apply(authRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryAddressUkController.questionPage().url

      }
    }

  "calling editCompany" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "be the same as showQuestion for a specific index" in {
      when(mockSessionService.fetchPartFromCompanyDetailsList[Company](any())(any(), any())).thenReturn(Future.successful(Some(Company("Test company",None,None))))

      val result = testController.editCompany(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_manual_company_details_uk.title"))
      contentAsString(result) should include("Test company")

    }
  }

  "calling editQuestionSubmit" should {
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "successfully bind the form and go to the edit version of the subsidiary address UK page with the index preserved if the form is filled correctly" in {
      when(mockSessionService.cache[Company](any(), any())(any(), any())).thenReturn(Future.successful(("","")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(Fixtures.exampleCompanies))
      doNothing().when(mockCompanyDetailsService).updateSubsidiaryCompanyCache(any())(any())

      val companyAddressData = Map("companyName" -> "Test person")
      val form = RsFormMappings.companyAddressUkForm().bind(companyAddressData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.editQuestionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryAddressUkController.editCompany(1).url
    }
  }
}
