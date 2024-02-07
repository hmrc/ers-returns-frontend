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

import controllers.schemeOrganiser.SchemeOrganiserController
import models._
import org.jsoup.Jsoup
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
import views.html.{global_error, scheme_organiser, scheme_organiser_summary}

import java.util.NoSuchElementException
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
  val schemeOrganiserView: scheme_organiser    = app.injector.instanceOf[scheme_organiser]
  val schemeOrganiserSummaryView: scheme_organiser_summary = app.injector.instanceOf[scheme_organiser_summary]

  "calling Scheme Organiser Page" should {

    def buildFakeSchemeOrganiserController(
      schemeOrganiserDetailsRes: Boolean = true,
      schemeOrganiserDataCached: Boolean = false,
      reportableEventsRes: Boolean = true,
      fileTypeRes: Boolean = true
    ): SchemeOrganiserController = new SchemeOrganiserController(
      mockMCC,
      mockAuthConnector,
      mockCountryCodes,
      mockErsUtil,
      mockAppConfig,
      globalErrorView,
      schemeOrganiserView,
      schemeOrganiserSummaryView,
      testAuthAction
    ) {

      when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(
        mockErsUtil.fetch[ReportableEvents](refEq(REPORTABLE_EVENTS), any())(any(), any())
      ).thenReturn(
        if (reportableEventsRes) {
          Future.successful(ReportableEvents(Some(OPTION_NO)))
        } else {
          Future.failed(new Exception)
        }
      )
      when(
        mockErsUtil.fetchOption[CheckFileType](refEq(FILE_TYPE_CACHE), any())(any(), any())
      ).thenReturn(
        if (fileTypeRes) {
          Future.successful(Some(CheckFileType(Some(OPTION_CSV))))
        } else {
          Future.failed(new NoSuchElementException)
        }
      )
      when(
        mockErsUtil.fetch[SchemeOrganiserDetails](refEq(SCHEME_ORGANISER_CACHE), any())(any(), any())
      ).thenReturn(
        if (schemeOrganiserDetailsRes) {
          if (schemeOrganiserDataCached) {
            Future.successful(
              SchemeOrganiserDetails("Name", Fixtures.companyName, None, None, None, None, None, None, None)
            )
          } else {
            Future.successful(SchemeOrganiserDetails("", "", None, None, None, None, None, None, None))
          }
        } else {
          Future.failed(new NoSuchElementException)
        }
      )
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserController()
      val result              = controllerUnderTest.schemeOrganiserPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserController()
      val result              = controllerUnderTest.schemeOrganiserPage().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching reportableEvents throws exception" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(reportableEventsRes = false)
      val req                 = Fixtures.buildFakeRequestWithSessionIdCSOP("GET")
      val authRequest         = buildRequestWithAuth(req)

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest, hc)
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeSchemeOrganiserController().getGlobalErrorPage(req, testMessages))
      )
    }

    "show blank scheme organiser page if fetching file type from cache fails" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(fileTypeRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest, hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=company-name]").hasText   shouldEqual false
      document.select("input[id=address-line-1]").hasText shouldEqual false
    }

    "show blank scheme organiser page if fetching scheme organiser details from cache fails" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDetailsRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest, hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=company-name]").hasText   shouldEqual false
      document.select("input[id=address-line-1]").hasText shouldEqual false
    }

    "show filled out scheme organiser page if fetching scheme organiser details from cache is successful" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCached = true)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest, hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=companyName]").`val`()  shouldEqual "Name"
      document.select("input[id=addressLine1]").`val`() shouldEqual Fixtures.companyName
    }

  }

  "calling Scheme Organiser Submit Page" should {

    def buildFakeSchemeOrganiserController(
      schemeOrganiserDetailsRes: Boolean = true,
      schemeOrganiserDataCached: Boolean = false,
      reportableEventsRes: Boolean = true,
      fileTypeRes: Boolean = true,
      schemeOrganiserDataCachedOk: Boolean = true
    ): SchemeOrganiserController = new SchemeOrganiserController(
      mockMCC,
      mockAuthConnector,
      mockCountryCodes,
      mockErsUtil,
      mockAppConfig,
      globalErrorView,
      schemeOrganiserView,
      schemeOrganiserSummaryView,
      testAuthAction
    ) {

      when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(mockErsUtil.fetch[ReportableEvents](refEq(REPORTABLE_EVENTS), any())(any(), any())).thenReturn(
        if (reportableEventsRes) {
          Future.successful(ReportableEvents(Some(OPTION_NO)))
        } else {
          Future.failed(new Exception)
        }
      )
      when(mockErsUtil.fetchOption[CheckFileType](refEq(FILE_TYPE_CACHE), any())(any(), any())).thenReturn(
        if (fileTypeRes) {
          Future.successful(Some(CheckFileType(Some(OPTION_CSV))))
        } else {
          Future.failed(new NoSuchElementException)
        }
      )
      when(mockErsUtil.fetch[SchemeOrganiserDetails](refEq(SCHEME_ORGANISER_CACHE), any())(any(), any())).thenReturn(
        if (schemeOrganiserDetailsRes) {
          if (schemeOrganiserDataCached) {
            Future.successful(
              SchemeOrganiserDetails("Name", Fixtures.companyName, None, None, None, None, None, None, None)
            )
          } else {
            Future.successful(SchemeOrganiserDetails("", "", None, None, None, None, None, None, None))
          }
        } else {
          Future.failed(new NoSuchElementException)
        }
      )
      when(mockErsUtil.cache(refEq(SCHEME_ORGANISER_CACHE), any(), any())(any(), any())).thenReturn(
        if (schemeOrganiserDataCachedOk) {
          Future.successful(null)
        } else {
          Future.failed(new Exception)
        }
      )

    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserController()
      val result              = controllerUnderTest.schemeOrganiserSubmit().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeSchemeOrganiserController()
      val result              = controllerUnderTest.schemeOrganiserSubmit().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "give a Ok status and stay on the same page if form errors and display the error" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController()
      val schemeOrganiserData = Map("" -> "")
      val form                = RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest, hc)
      status(result) shouldBe Status.OK
    }

  }

}
