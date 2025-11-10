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

package controllers

import controllers.schemeOrganiser.SchemeOrganiserController
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, scheme_organiser_summary}

import scala.concurrent.{ExecutionContext, Future}

class SchemeOrganiserControllerSpec
    extends AnyWordSpecLike
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
  val globalErrorView: global_error            = app.injector.instanceOf[global_error]
  val schemeOrganiserSummaryView: scheme_organiser_summary = app.injector.instanceOf[scheme_organiser_summary]

  val companyDetails = CompanyDetails("First company", "20 Garden View", None, None, None, None, None, None, None, true)

  "calling Scheme Organiser summary page" should {

    val failure: Future[Nothing] = Future.failed(new Exception("failure"))

    def buildFakeSchemeOrganiserSummaryController(
                                                   schemeOrganiserSummaryRes: Boolean = true,
                                                   schemeOrganiserSummaryCached: Boolean = false,
                                                   reportableEventsRes: Boolean = true,
                                                   fileTypeRes: Boolean = true,
                                                   schemeOrganiserSummaryCachedOk: Boolean = true,
                                                   requestObjectRes: Future[RequestObject] = Future.successful(ersRequestObject)
                                                 ): SchemeOrganiserController = new SchemeOrganiserController(
      mockMCC,
      mockCountryCodes,
      mockErsUtil,
      mockSessionService,
      mockAppConfig,
      globalErrorView,
      schemeOrganiserSummaryView,
      testAuthAction
    ){


      when(mockSessionService.fetch[CompanyDetails](refEq(mockErsUtil.SCHEME_ORGANISER_CACHE))(any(), any())).thenReturn(
        if (schemeOrganiserSummaryRes) {
          if (schemeOrganiserSummaryCached) {
            Future.successful(
              CompanyDetails("Name", "123 Street", None, None, None, Some("UK"), None, None, None, true)
            )
          } else {
            Future.successful(CompanyDetails("", "", None, None, None, None, None, None, None, false))
          }
        } else {
          Future.failed(new NoSuchElementException)
        }
      )

      when(mockSessionService.cache(refEq(SCHEME_ORGANISER_CACHE), any())(any(), any())).thenReturn(
        if (schemeOrganiserSummaryCachedOk) {
          Future.successful(null)
        } else {
          Future.failed(new Exception)
        }
      )

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(requestObjectRes)

      when(mockSessionService.fetch[ReportableEvents](refEq(REPORTABLE_EVENTS))(any(), any())).thenReturn(
        if (reportableEventsRes) {
          Future.successful(ReportableEvents(Some(OPTION_NO)))
        } else {
          Future.failed(new Exception)
        }
      )
      when(mockSessionService.fetchOption[CheckFileType](refEq(FILE_TYPE_CACHE), any())(any(), any())).thenReturn(
        if (fileTypeRes) {
          Future.successful(Some(CheckFileType(Some(OPTION_CSV))))
        } else {
          Future.failed(new NoSuchElementException)
        }
      )
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController()
      val result              = controllerUnderTest.schemeOrganiserSummaryPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController()
      val result              = controllerUnderTest.schemeOrganiserSummaryPage().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching schemeOrganiser company details fails" in {
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController(schemeOrganiserSummaryRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showSchemeOrganiserSummaryPage(authRequest)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "direct to ers errors page if fetching request object fails" in {
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController(requestObjectRes = failure)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      contentAsString(controllerUnderTest.showSchemeOrganiserSummaryPage(authRequest)) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }


    "display ers errors page if scheme organiser summary page pre-filled" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController(schemeOrganiserSummaryCached = true)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserSummaryPage(authRequest)

      contentAsString(result) shouldBe contentAsString(
        Future(controllerUnderTest.getGlobalErrorPage(testFakeRequest, testMessages))
      )
      status(result) shouldBe Status.OK

    }

    "return Ok with schemeOrganiserSummaryView" in {

      when(mockSessionService.fetchSchemeOrganiserOptionally()(any(),any()))
        .thenReturn(Future.successful(Some(companyDetails)))
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController(schemeOrganiserSummaryCached = true)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserSummaryPage(authRequest)
      status(result) shouldBe Status.OK

    }

    "redirect to SchemeOrganiserNameController.questionPage if no scheme organiser is present" in {

      when(mockSessionService.fetchSchemeOrganiserOptionally()(any(),any())).thenReturn(Future.successful(None))
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController(schemeOrganiserSummaryCached = false)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserSummaryPage(authRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/where-is-company-registered")
    }

    "continue button gives a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController()

      val result = controllerUnderTest.companySummaryContinue().apply(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
      headers(result) should contain(("Location" -> "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9290%2Fsubmit-your-ers-annual-return&origin=ers-returns-frontend"))
    }

    "redirect to group scheme page when companySummaryContinue is called" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserSummaryController()

      val result = controllerUnderTest.companySummaryContinue().apply(Fixtures.buildFakeRequestWithSessionIdSIP("GET").withFormUrlEncodedBody("addCompany" -> "0"))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/group-scheme")
    }
  }

}
