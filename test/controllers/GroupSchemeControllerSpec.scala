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

import forms.YesNoFormProvider
import models._
import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html._

import scala.concurrent.{ExecutionContext, Future}

class GroupSchemeControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ErsTestHelper
    with ERSFakeApplicationConfig
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite {

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

  implicit lazy val materializer: Materializer = app.materializer
  val globalErrorView: global_error = app.injector.instanceOf[global_error]
  val groupView: group = app.injector.instanceOf[group]
  val manualCompanyDetailsView: manual_company_details = app.injector.instanceOf[manual_company_details]
  val groupPlanSummaryView: group_plan_summary = app.injector.instanceOf[group_plan_summary]
  val confirmDeleteCompanyView: confirm_delete_company = app.injector.instanceOf[confirm_delete_company]
  val yesNoFormProvider: YesNoFormProvider = app.injector.instanceOf[YesNoFormProvider]

  val company: CompanyDetails =
    CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, Some("UK"), None, None, None)
  lazy val companyDetailsList: CompanyDetailsList = CompanyDetailsList(List(company, company))
  lazy val companyDetailsListSingle: CompanyDetailsList = CompanyDetailsList(List(company))
  lazy val companyDetailsListEmpty: CompanyDetailsList = CompanyDetailsList(List())

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockErsUtil, mockSessionService, mockCompanyService)
    when(mockErsUtil.GROUP_SCHEME_COMPANIES).thenReturn("group-scheme-companies")
    when(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER).thenReturn("group-scheme-controller")
    when(mockErsUtil.ERS_REQUEST_OBJECT).thenReturn("ers-request-object")
    when(mockErsUtil.OPTION_MANUAL).thenReturn("man")
    when(mockErsUtil.DEFAULT).thenReturn("")
    when(mockErsUtil.SCHEME_CSOP).thenReturn("1")
    when(mockErsUtil.SCHEME_EMI).thenReturn("2")
    when(mockErsUtil.SCHEME_OTHER).thenReturn("3")
    when(mockErsUtil.SCHEME_SAYE).thenReturn("4")
    when(mockErsUtil.SCHEME_SIP).thenReturn("5")
    when(mockErsUtil.OPTION_YES).thenReturn("1")
    when(mockErsUtil.OPTION_NO).thenReturn("2")
    when(mockErsUtil.getPageElement(any(), any(), any(), any())(any())).thenCallRealMethod()
    when(mockErsUtil.companyLocation(any())).thenReturn("UK")
    when(mockErsUtil.buildAddressSummary(any())).thenReturn("addressSummary")
    when(mockErsUtil.MSG_CSOP).thenReturn(".csop.")
    when(mockErsUtil.PAGE_GROUP_SUMMARY).thenReturn("ers_group_summary")
  }

  lazy val testGroupSchemeController: GroupSchemeController = new GroupSchemeController(
    mockMCC,
    mockSessionService,
    mockCompanyService,
    globalErrorView,
    groupView,
    manualCompanyDetailsView,
    groupPlanSummaryView,
    confirmDeleteCompanyView,
    yesNoFormProvider,
    testAuthAction
  )

  "manualCompanyDetailsPage" should {

    def manualCompanyDetailsPageHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.manualCompanyDetailsPage(index).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      manualCompanyDetailsPageHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result)                                                  shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showManualCompanyDetailsPage" should {
    "display company details page for correct scheme" in {
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = testGroupSchemeController.showManualCompanyDetailsPage(1)(authRequest)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers_manual_company_details.csop.title") + " " + Messages("ers.title.postfix")
    }
  }

  "manualCompanyDetailsSubmit" should {
    def manualCompanyDetailsSubmitHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.manualCompanyDetailsSubmit(index).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      manualCompanyDetailsSubmitHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result)                                                  shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showManualCompanyDetailsSubmit" should {

    def buildCompanyDetailsRequest(isValid: Boolean) = {
      val data = if (isValid) {
        Map(
          "companyName" -> Fixtures.companyName,
          "addressLine1" -> "Add1",
          "addressLine2" -> "Add2",
          "addressLine3" -> "Add3",
          "addressLine4" -> "Add4",
          "postcode" -> "AA111AA",
          "country" -> "United Kingdom",
          "companyReg" -> "",
          "corporationRef" -> ""
        )
      } else {
        Map("" -> "")
      }
      val form = _root_.models.RsFormMappings.companyDetailsForm().bind(data)
      Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
    }

    "display error if showManualCompanyDetailsSubmit is called with authentication and form errors" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = false))
      setAuthMocks()
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      val result = testGroupSchemeController.manualCompanyDetailsSubmit(1000)(authRequest)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").isEmpty shouldBe false
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data entered for 1st company" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 10000)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/subsidiary-company-summary"
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data for additional company" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/subsidiary-company-summary"
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data for updated company" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 0)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/subsidiary-company-summary"
    }

    "redirect to Group Summary page if data is filled correctly and there is nothing in existing cache" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException("Nothing in cache")))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1000)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/subsidiary-company-summary"
    }
  }

  "confirmDeleteCompanyPage" should {
    def confirmDeleteCompanyHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(handler: Future[Result] => Any): Unit =
      handler(testGroupSchemeController.confirmDeleteCompanyPage(index).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()

      confirmDeleteCompanyHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showConfirmDeleteCompany" should {
    "direct to ers errors page if fetchAll fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      when(mockSessionService.fetchAll()(any())) thenReturn Future.failed(new Exception("error"))

      val result = testGroupSchemeController.showConfirmDeleteCompany(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "show confirmDeleteCompanyPage with the selected company if called with authentication and correct cache" in {
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val result = testGroupSchemeController.showConfirmDeleteCompany(0)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h1").text shouldBe Messages("ers.group_confirm_delete_company.page_header", Fixtures.companyName)
    }

    "show confirmDeleteCompanyPage with no extra info if this is not the last company" in {
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val result = testGroupSchemeController.showConfirmDeleteCompany(0)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getAllElements.text.contains(Messages("ers.group_confirm_delete_company.page_body_1")) shouldBe false
      document.getAllElements.text.contains(Messages("ers.group_confirm_delete_company.page_body_2")) shouldBe false
      document.getAllElements.text.contains(Messages("validation.summary.heading")) shouldBe false
      document.getAllElements.text.contains(Messages("ers.group_confirm_delete_company.err.message")) shouldBe false
    }

    "show confirmDeleteCompanyPage with extra info if this is the last company" in {
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsListSingle)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val result = testGroupSchemeController.showConfirmDeleteCompany(0)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("final-page-body-1").text shouldBe Messages("ers.group_confirm_delete_company.page_body_1")
      document.getElementById("final-page-body-2").text shouldBe Messages("ers.group_confirm_delete_company.page_body_2")
      document.getAllElements.text.contains(Messages("validation.summary.heading")) shouldBe false
      document.getAllElements.text.contains(Messages("ers.group_confirm_delete_company.err.message")) shouldBe false
    }

    "filter deleted company before caching and redirecting" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)

      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      val result = testGroupSchemeController.showConfirmDeleteCompany(0)(authRequest)

      status(result) shouldBe OK
    }
  }

  "confirmDeleteCompanySubmit" should {
    "give a redirect to groupPlanSummaryPage if the company is deleted but more remain" in {
      setAuthMocks()
      val authRequest =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(("value", "true")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockCompanyService.deleteCompany(any())(any())).thenReturn(Future.successful(true))

      val result = testGroupSchemeController.confirmDeleteCompanySubmit(0)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/subsidiary-company-summary"
    }

    "give a redirect to groupSchemePage if the final company is deleted" in {
      setAuthMocks()
      val authRequest =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(("value", "true")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsListSingle))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))
      when(mockCompanyService.deleteCompany(any())(any())).thenReturn(Future.successful(true))

      val result = testGroupSchemeController.confirmDeleteCompanySubmit(0)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/group-scheme"
    }

    "show confirmDeleteCompanyView with form errors if form is empty" in {
      setAuthMocks()
      val authRequest =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(("value", "")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val result = testGroupSchemeController.confirmDeleteCompanySubmit(0)(authRequest)

      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h1").text shouldBe Messages("ers.group_confirm_delete_company.page_header", Fixtures.companyName)
      document.getElementsByClass("govuk-error-summary__title").text shouldBe Messages("validation.summary.heading")
      document.getElementById("value-error").text shouldBe "Error: " + Messages("ers.group_confirm_delete_company.err.message")
    }

    "give a redirect to groupSchemePage if the 'No' radio option is selected" in {
      setAuthMocks()
      val authRequest =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(("value", "false")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val result = testGroupSchemeController.confirmDeleteCompanySubmit(0)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/subsidiary-company-summary"
    }

    "give a redirect to getGlobalErrorPage if the company is fails to delete" in {
      setAuthMocks()
      val authRequest =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(("value", "true")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))
      when(mockCompanyService.deleteCompany(any())(any())).thenReturn(Future.successful(false))

      val result = testGroupSchemeController.confirmDeleteCompanySubmit(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "give a redirect to getGlobalErrorPage if the final company fails to delete" in {
      setAuthMocks()
      val authRequest =
        buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(("value", "true")))
      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsListSingle))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))
      when(mockCompanyService.deleteCompany(any())(any())).thenReturn(Future.successful(false))

      val result = testGroupSchemeController.confirmDeleteCompanySubmit(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }
  }

  "editCompany" should {
    def editCompanyHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.editCompany(index).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()

      editCompanyHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showEditCompany" should {
    "display error page if fetch company details list fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn (Future.failed(new NoSuchElementException("Nothing in cache")))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      val result = testGroupSchemeController.showEditCompany(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "display error page if fetch request object fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val result = testGroupSchemeController.showEditCompany(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "display manualCompanyDetailsPage with the selected company details if showEditCompany is called with authentication and correct cache" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))

      val result = testGroupSchemeController.showEditCompany(0)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers_manual_company_details.csop.title") + " " + Messages("ers.title.postfix")
    }
  }

  "groupSchemePage" should {
    def groupSchemePageHandler(request: FakeRequest[AnyContentAsEmpty.type])(handler: Future[Result] => Any): Unit =
      handler(testGroupSchemeController.groupSchemePage().apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      groupSchemePageHandler(Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }

  }

  "groupSchemePage" should {
    "display group scheme page if there is no data in cache" in {
      setAuthMocks()
      when(mockSessionService.fetch[GroupSchemeInfo](refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER))(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException("Nothing in cache")))
      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.groupSchemePage()(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=yes]").hasAttr("checked") shouldEqual false
      document.select("input[id=no]").hasAttr("checked")  shouldEqual false
    }
    "display group scheme page if there is cached data" in {
      when(mockSessionService.fetch[GroupSchemeInfo](refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER))(any(), any()))
        .thenReturn(Future.successful(GroupSchemeInfo(Option(OPTION_YES), None)))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.showGroupSchemePage(ersRequestObject)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=yes]").hasAttr("checked") shouldEqual true
      document.select("input[id=no]").hasAttr("checked")  shouldEqual false
    }
  }

  "groupSchemeSelected" should {
    def groupSchemeSelectedHandler(scheme: String, request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.groupSchemeSelected(scheme).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      groupSchemeSelectedHandler("", Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result)                                                  shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showGroupSchemeSelected" should {
    def buildGroupSchemeSelectedRequest(result: Option[Boolean], scheme: String) = {
      val data = result match {
        case None => Map("" -> "")
        case Some(true) => Map("groupScheme" -> OPTION_YES)
        case Some(false) => Map("groupScheme" -> OPTION_NO)
      }
      val form = _root_.models.RsFormMappings.groupForm().bind(data)
      val request = scheme match {
        case "CSOP" | "" => Fixtures.buildFakeRequestWithSessionIdCSOP("POST")
        case "SAYE" => Fixtures.buildFakeRequestWithSessionIdSAYE("POST")
        case "EMI" => Fixtures.buildFakeRequestWithSessionIdEMI("POST")
        case "SIP" => Fixtures.buildFakeRequestWithSessionIdSIP("POST")
        case "OTHER" => Fixtures.buildFakeRequestWithSessionIdOTHER("POST")
      }
      request.withFormUrlEncodedBody(form.data.toSeq: _*)
    }

    "display errors if invalid data is sent" in {
      setAuthMocks()
      val request = buildGroupSchemeSelectedRequest(None, "CSOP")
      val authRequest = buildRequestWithAuth(request)
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val result = testGroupSchemeController.groupSchemeSelected(SCHEME_CSOP)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h2").get(0).text shouldBe Messages("validation.summary.heading")
    }

    "display errors if no data is set" in {
      val request = buildGroupSchemeSelectedRequest(None, "")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h2").get(0).text shouldBe Messages("validation.summary.heading")
    }

    "redirect to company details page if user select yes for CSOP" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "CSOP")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/add-subsidiary-company"
    }

    "redirect to alterations page if user select no for CSOP" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "CSOP")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject.copy(schemeType = Some("CSOP")), SCHEME_CSOP)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/alterations-or-a-variation"
    }

    "redirect to company details page if user select yes for SAYE" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "SAYE")
      val authRequest = buildRequestWithAuth(request)

      val result =
        testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_SAYE)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/add-subsidiary-company"
    }

    "redirect to alterations page if user select no for SAYE" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "SAYE")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject.copy(schemeType = Some("SAYE")), mockErsUtil.SCHEME_SAYE)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/alterations-or-a-variation"
    }

    "redirect to company details page if user select yes for EMI" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "EMI")
      val authRequest = buildRequestWithAuth(request)

      val result =
        testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_EMI)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/add-subsidiary-company"
    }

    "redirect to summary page if user select no for EMI" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "EMI")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject.copy(schemeType = Some("EMI")), mockErsUtil.SCHEME_EMI)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/annual-return-summary"
    }

    "redirect to company details page if user select yes for SIP" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "SIP")
      val authRequest = buildRequestWithAuth(request)

      val result =
        testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_SIP)(authRequest)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/add-subsidiary-company"
    }

    "redirect to trustee summary page if user select no for SIP" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request     = buildGroupSchemeSelectedRequest(Some(false), "SIP")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject.copy(schemeType = Some("SIP")), mockErsUtil.SCHEME_SIP)(authRequest)
      status(result) shouldBe SEE_OTHER

      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/trustees"
    }

    "redirect to company details page if user select yes for OTHER" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request     = buildGroupSchemeSelectedRequest(Some(true), "OTHER")
      val authRequest = buildRequestWithAuth(request)

      val result =
        testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_OTHER)(authRequest)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/add-subsidiary-company"
    }

    "redirect to summary page if user select no for OTHER" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request     = buildGroupSchemeSelectedRequest(Some(false), "OTHER")
      val authRequest = buildRequestWithAuth(request)

      val result =
        testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_OTHER)(authRequest)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/annual-return-summary"
    }

  }

  "groupPlanSummaryPage" should {
    def groupPlanSummaryPageHandler(request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.groupPlanSummaryPage().apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      groupPlanSummaryPageHandler(Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result)                                                  shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showGroupPlanSummaryPage" should {

    "display error page if fetch company details list fails" in {
      setAuthMocks()
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException("Nothing in cache")))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.groupPlanSummaryPage()(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "display error page if company list empty" in {
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsListEmpty))

      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "display error page if fetch request object fails" in {
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("title").text shouldBe Messages("ers.global_errors.title")
    }

    "display group plan summary if showGroupPlanSummaryPage is called with authentication and correct cache" in {
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))

      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject.copy(
          schemeName = Some("CSOP"),
          schemeType = Some("CSOP")
        )))


      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h1").text shouldBe Messages("ers_group_summary.csop.title")
    }
  }

  "groupPlanSummaryContinue" should {
    def groupPlanSummaryContinueHandler(scheme: String, request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.groupPlanSummaryContinue(scheme).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      groupPlanSummaryContinueHandler("", Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result)                                                  shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "continueFromGroupPlanSummaryPage" should {
    "redirect to alterations page for CSOP" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_CSOP)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/alterations-or-a-variation"
    }

    "redirect to alterations page for SAYE" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SAYE)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/alterations-or-a-variation"
    }

    "redirect to summary page for EMI" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_EMI)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/annual-return-summary"
    }

    "redirect to trustee page for SIP" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SIP)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/trustees"
    }

    "redirect to summary page for OTHER" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_OTHER)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location") shouldBe "/submit-your-ers-annual-return/annual-return-summary"
    }
  }
}
