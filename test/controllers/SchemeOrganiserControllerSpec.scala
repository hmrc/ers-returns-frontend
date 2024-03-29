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
import views.html.{global_error, scheme_organiser}

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

  "calling Scheme Organiser Page" should {

    def buildFakeSchemeOrganiserController(
      schemeOrganiserDetailsRes: Boolean = true,
      schemeOrganiserDataCached: Boolean = false,
      reportableEventsRes: Boolean = true,
      fileTypeRes: Boolean = true
    ): SchemeOrganiserController = new SchemeOrganiserController(
      mockMCC,
      mockSessionService,
      globalErrorView,
      schemeOrganiserView,
      testAuthAction
    ) {

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

      when(
        mockSessionService.fetch[ReportableEvents](refEq(REPORTABLE_EVENTS))(any(), any())
      ).thenReturn(
        if (reportableEventsRes) {
          Future.successful(ReportableEvents(Some(OPTION_NO)))
        } else {
          Future.failed(new Exception)
        }
      )
      when(
        mockSessionService.fetchOption[CheckFileType](refEq(FILE_TYPE_CACHE), any())(any(), any())
      ).thenReturn(
        if (fileTypeRes) {
          Future.successful(Some(CheckFileType(Some(OPTION_CSV))))
        } else {
          Future.failed(new NoSuchElementException)
        }
      )
      when(
        mockSessionService.fetch[SchemeOrganiserDetails](refEq(SCHEME_ORGANISER_CACHE))(any(), any())
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

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest)
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeSchemeOrganiserController().getGlobalErrorPage(req, testMessages))
      )
    }

    "show blank scheme organiser page if fetching file type from cache fails" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(fileTypeRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=company-name]").hasText   shouldEqual false
      document.select("input[id=address-line-1]").hasText shouldEqual false
    }

    "show blank scheme organiser page if fetching scheme organiser details from cache fails" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDetailsRes = false)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=company-name]").hasText   shouldEqual false
      document.select("input[id=address-line-1]").hasText shouldEqual false
    }

    "show filled out scheme organiser page if fetching scheme organiser details from cache is successful" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCached = true)
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      val result = controllerUnderTest.showSchemeOrganiserPage(ersRequestObject)(authRequest)
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
      mockSessionService,
      globalErrorView,
      schemeOrganiserView,
      testAuthAction
    ) {

      when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

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
      when(mockSessionService.fetch[SchemeOrganiserDetails](refEq(SCHEME_ORGANISER_CACHE))(any(), any())).thenReturn(
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
      when(mockSessionService.cache(refEq(SCHEME_ORGANISER_CACHE), any())(any(), any())).thenReturn(
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

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      status(result) shouldBe Status.OK
    }

    "give a redirect status on POST if no form errors" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController()
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine"    -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine1"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      status(result)                                shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "direct to ers errors page if saving scheme organiser data throws exception" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine"    -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine1"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeSchemeOrganiserController().getGlobalErrorPage(request, testMessages))
      )
    }

    "check error for empty company name" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> "",
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.summary.company_name_required"))
    }
    "check error for company name more than 36 characters" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> "Company Name more than thirty six characters",
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.company_name"))
    }

    "check error for invalid company name" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> "Inv@lid Company name",
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.invalidChars.company_name"))
    }

    "check error for empty Address" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.summary.address_line1_required"))
    }

    "check error for address more than 28 characters" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1 more than Twenty Eight characters",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.address_line1"))
    }

    "check error for invalid address" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1 Inv@lid",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.invalidChars.address_line1"))
    }

    "check error for postcode more than 8 characters" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AAAA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.postcode"))
    }

    "check error for invalid postcode" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11*1A",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.invalidChars.postcode"))
    }

    "check error for CRN more than 8 characters" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB12345612",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.summary.company_reg"))
    }

    "check error for invalid CRN" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB12345)",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.summary.company_reg"))
    }

    "check error for corporation number more than 10 digits" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "12345678901"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.summary.corporation_ref"))
    }

    "check error for invalid corporation number" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "AA11 1AA",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567-89"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(
        testMessages("ers_scheme_organiser.err.summary.invalidChars.corporation_ref_pattern")
      )
    }

    "check error for invalid format of postcode" in {
      val controllerUnderTest = buildFakeSchemeOrganiserController(schemeOrganiserDataCachedOk = false)
      val schemeOrganiserData = Map(
        "companyName"    -> Fixtures.companyName,
        "addressLine1"   -> "Add1",
        "addressLine2"   -> "Add2",
        "addressLine3"   -> "Add3",
        "addressLine4"   -> "Add4",
        "postcode"       -> "123456",
        "country"        -> "United Kingdom",
        "companyReg"     -> "AB123456",
        "corporationRef" -> "1234567890"
      )
      val form                = _root_.models.RsFormMappings.schemeOrganiserForm().bind(schemeOrganiserData)
      val request             = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showSchemeOrganiserSubmit(ersRequestObject)(authRequest)
      contentAsString(result) should include(testMessages("ers_scheme_organiser.err.invalidFormat.postcode"))
    }

  }

}
