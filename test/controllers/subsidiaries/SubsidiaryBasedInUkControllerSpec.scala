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

package controllers.subsidiaries

import models.{CompanyBasedInUk, RequestObject, RsFormMappings}
import org.mockito.ArgumentMatchers.{any, refEq}
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
import views.html.{global_error, manual_is_the_company_in_uk}

import scala.concurrent.{ExecutionContext, Future}

class SubsidiaryBasedInUkControllerSpec extends AnyWordSpecLike
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


  val testController = new SubsidiaryBasedInUkController (
    mockMCC,
    mockAuthConnector,
    mockErsConnector,
    app.injector.instanceOf[global_error],
    testAuthAction,
    mockCountryCodes,
    mockErsUtil,
    mockSessionService,
    mockAppConfig,
    app.injector.instanceOf[manual_is_the_company_in_uk]
  )

  "calling showQuestionPage" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "show the empty subsidiary based in UK question page when there is nothing to prefill" in {
      when(mockSessionService.fetchPartFromCompanyDetailsList[CompanyBasedInUk](any())(any(), any())).thenReturn(Future.successful(None))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_manual_is_the_company_in_uk.title"))
      contentAsString(result) should include(testMessages("ers_manual_company_details.uk"))
      contentAsString(result) should include(testMessages("ers_manual_company_details.overseas"))
    }

    "show the prefilled subsidiary based in UK question page when there is data to prefill" in {
      when(mockSessionService.fetchPartFromCompanyDetailsList[CompanyBasedInUk](any())(any(), any())).thenReturn(Future.successful(Some(CompanyBasedInUk(true))))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_manual_is_the_company_in_uk.title"))
      contentAsString(result) should include(testMessages("ers_manual_company_details.uk"))
      contentAsString(result) should include("value=\"0\"")
      contentAsString(result) should include("checked")
    }

    "show the global error page if an exception occurs while retrieving cached data" in {
      when(mockSessionService.fetchPartFromCompanyDetailsList[CompanyBasedInUk](any())(any(), any())).thenReturn(Future.failed(new RuntimeException("Failure scenario")))

      val result = testController.questionPage(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

  "calling handleQuestionSubmit" should {
    "show the subsidiary based in UK form page with errors if the form is incorrectly filled" in {
      val companyBasedData = Map("bool" -> "")
      val form = RsFormMappings.companyBasedInUkForm().bind(companyBasedData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.questionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include(testMessages("ers_manual_is_the_company_in_uk.title"))
      contentAsString(result) should include(testMessages("error.required"))
    }
  }

  "successfully bind the form and go to the subsidiary address UK page if true" in {
    when(mockSessionService.cache[CompanyBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
    when(mockSessionService.fetch[CompanyBasedInUk](refEq(mockErsUtil.SUBSIDIARY_COMPANY_BASED))(any(), any())).thenReturn(Future.successful(CompanyBasedInUk(true)))
    doNothing().when(mockCompanyDetailsService).updateSubsidiaryCompanyCache(any())(any())

    val companyBasedData = Map("basedInUk" -> "0")
    val form = RsFormMappings.companyBasedInUkForm().bind(companyBasedData)
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
    val result = testController.questionSubmit(1).apply(authRequest)

    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryDetailsUkController.questionPage().url
  }

  "successfully bind the form and go to the scheme organiser address overseas page if false" in {
    when(mockSessionService.cache[CompanyBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
    when(mockSessionService.fetch[CompanyBasedInUk](refEq(mockErsUtil.SUBSIDIARY_COMPANY_BASED))(any(), any())).thenReturn(Future.successful(CompanyBasedInUk(false)))
    doNothing().when(mockCompanyDetailsService).updateSubsidiaryCompanyCache(any())(any())

    val trusteeBasedData = Map("basedInUk" -> "1")
    val form = RsFormMappings.companyBasedInUkForm().bind(trusteeBasedData)
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
    val result = testController.questionSubmit(1).apply(authRequest)

    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryDetailsOverseasController.questionPage().url
  }


  "calling editQuestion" should {
    implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "be the same as showQuestion for a specific index" in {
      when(mockSessionService.fetchPartFromCompanyDetailsList[CompanyBasedInUk](any())(any(), any())).thenReturn(Future.successful(Some(CompanyBasedInUk(false))))

      val result = testController.editCompany(1).apply(authRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include(testMessages("ers_manual_is_the_company_in_uk.title"))
    }
  }

  "calling editQuestionSubmit" should {
    setAuthMocks()
    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "successfully bind the form and go to the edit version of the scheme organiser address UK page with the index preserved if the answer is true" in {

      when(mockSessionService.cache[CompanyBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
      when(mockSessionService.fetch[CompanyBasedInUk](refEq(mockErsUtil.SUBSIDIARY_COMPANY_BASED))(any(), any())).thenReturn(Future.successful(CompanyBasedInUk(true)))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(Fixtures.exampleCompanies))
      doNothing().when(mockCompanyDetailsService).updateSubsidiaryCompanyCache(any())(any())

      val companyBasedData = Map("basedInUk" -> "0")
      val form = RsFormMappings.companyBasedInUkForm().bind(companyBasedData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.editQuestionSubmit(0).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryDetailsUkController.editCompany(0).url
    }

    "successfully bind the form and go to the edit version of the scheme organiser address overseas page with the index preserved if the answer is false" in {

      when(mockSessionService.cache[CompanyBasedInUk](any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
      when(mockSessionService.fetch[CompanyBasedInUk](refEq(mockErsUtil.SUBSIDIARY_COMPANY_BASED))(any(), any())).thenReturn(Future.successful(CompanyBasedInUk(false)))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(Fixtures.exampleCompanies))
      doNothing().when(mockCompanyDetailsService).updateSubsidiaryCompanyCache(any())(any())

      val companyBasedData = Map("basedInUk" -> "0")
      val form = RsFormMappings.companyBasedInUkForm().bind(companyBasedData)
      implicit val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*))
      val result = testController.editQuestionSubmit(1).apply(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryDetailsOverseasController.editCompany(1).url
    }
  }
}
