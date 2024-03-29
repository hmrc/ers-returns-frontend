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

import org.apache.pekko.stream.Materializer
import models._
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
import views.html.{global_error, group, group_plan_summary, manual_company_details}

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

  val company: CompanyDetails =
    CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, Some("UK"), None, None, None)
  lazy val companyDetailsList: CompanyDetailsList = CompanyDetailsList(List(company, company))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockErsUtil, mockSessionService)
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
  }

  lazy val testGroupSchemeController: GroupSchemeController = new GroupSchemeController(
    mockMCC,
    mockSessionService,
    globalErrorView,
    groupView,
    manualCompanyDetailsView,
    groupPlanSummaryView,
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
        .thenReturn(Future.successful(ersRequestObject))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val result      = testGroupSchemeController.showManualCompanyDetailsPage(1)(authRequest)
      status(result) shouldBe OK
      contentAsString(result).contains(Messages("ers_manual_company_details.csop.title"))
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
      contentAsString(result) should include ("govuk-error-summary")
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data entered for 1st company" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 10000)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/group-summary")
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data for additional company" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/group-summary")
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data for updated company" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 0)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/group-summary")
    }

    "redirect to Group Summary page if data is filled correctly and there is nothing in existing cache" in {
      val authRequest = buildRequestWithAuth(buildCompanyDetailsRequest(isValid = true))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException("Nothing in cache")))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1000)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/group-summary")
    }
  }

  "calling replace company" should {

    lazy val controllerUnderTest: GroupSchemeController = new GroupSchemeController(
      mockMCC,
      mockSessionService,
      globalErrorView,
      groupView,
      manualCompanyDetailsView,
      groupPlanSummaryView,
      testAuthAction
    )

    "replace a companies and keep the other companies" when {
      "given an index that matches a companies in the list" in {
        val index = 2
        val formData = CompanyDetails("Replacement Company", "1 Some Place", None, None, None, None, None, None, None)
        val target   = CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None)

        val companiesDetailsList = List(
          CompanyDetails("First Company", "20 Garden View", None, None, None, None, None, None, None),
          CompanyDetails("Third Company", "72 Big Avenue", None, None, None, None, None, None, None),
          target,
          CompanyDetails("Fourth Company", "21 Brick Lane", None, None, None, None, None, None, None)
        )

        val result = controllerUnderTest.replaceCompany(companiesDetailsList, index, formData)

        result should contain(formData)
        result shouldNot contain(target)
        result.length shouldBe 4
      }
    }

    "keep the existing list of companies" when {
      "given an index that does not match any existing companies" in {
        val index = 100
        val formData = CompanyDetails("Replacement Company", "1 Some Place", None, None, None, None, None, None, None)
        val target   = CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None)

        val companyDetailsList = List(
          CompanyDetails("First Company", "20 Garden View", None, None, None, None, None, None, None),
          CompanyDetails("Third Company", "72 Big Avenue", None, None, None, None, None, None, None),
          target,
          CompanyDetails("Fourth Company", "21 Brick Lane", None, None, None, None, None, None, None)
        )

        val result = controllerUnderTest.replaceCompany(companyDetailsList, index, formData)

        result shouldNot contain(formData)
        result should contain(target)
        result.length shouldBe 4
      }
    }

    "remove duplicate records" when {
      "duplicates are present" in {
        val index = 1
        val target = CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None)
        val companyDetailsList = List(
          target,
          target,
          target,
          target
        )

        val result = controllerUnderTest.replaceCompany(companyDetailsList, index, target)

        result should contain(target)
        result.length shouldBe 1
      }
    }
  }

  "deleteCompany" should {
    def deleteCompanyHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(handler: Future[Result] => Any): Unit =
      handler(testGroupSchemeController.deleteCompany(index).apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()

      deleteCompanyHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showDeleteCompany" should {
    "direct to ers errors page if fetchAll fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))

      when(mockSessionService.fetchAll()(any())) thenReturn Future.failed(new Exception("error"))

      val result = testGroupSchemeController.showDeleteCompany(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "give a redirect to groupPlanSummaryPage with the selected company deleted if showDeleteCompany is called with authentication and correct cache" in {
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any())).thenReturn(Future(sessionPair))

      val result = testGroupSchemeController.showDeleteCompany(0)(authRequest)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/group-summary")
    }

    "filter deleted company before caching and redirecting" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      val expected = CompanyDetailsList(List(company))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future(sessionPair))

      val result = testGroupSchemeController.showDeleteCompany(0)(authRequest)

      status(result) shouldBe SEE_OTHER

      verify(mockSessionService, times(1)).cache(refEq(GROUP_SCHEME_COMPANIES), refEq(expected))(any(), any())
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
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display error page if fetch request object fails" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val result = testGroupSchemeController.showEditCompany(0)(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display manualCompanyDetailsPage with the selected company details if showEditCompany is called with authentication and correct cache" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))

      val result = testGroupSchemeController.showEditCompany(0)(authRequest)

      status(result) shouldBe OK
      contentAsString(result).contains(Messages("ers_manual_company_details.csop.title"))
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
        .thenReturn(Future.successful(ersRequestObject))

      val result = testGroupSchemeController.groupSchemeSelected(SCHEME_CSOP)(authRequest)

      status(result) shouldBe OK
      contentAsString(result).contains(Messages("validation.summary.heading")) shouldBe true
    }

    "display errors if no data is set" in {
      val request = buildGroupSchemeSelectedRequest(None, "")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(authRequest)

      status(result) shouldBe OK
      contentAsString(result).contains(Messages("validation.summary.heading")) shouldBe true
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
      headers(result).get("Location").get.contains("/add-subsidiary-company") shouldBe true
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
      headers(result).get("Location").get.contains("/alterations-or-a-variation") shouldBe true
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
      headers(result).get("Location").get.contains("/add-subsidiary-company") shouldBe true
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
      headers(result).get("Location").get.contains("/alterations-or-a-variation")
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
      headers(result).get("Location").get.contains("/add-subsidiary-company") shouldBe true
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
      headers(result).get("Location").get.contains("/annual-return-summary") shouldBe true
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
      headers(result).get("Location").get.contains("/add-subsidiary-company") shouldBe true
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

      headers(result).get("Location").get.contains("/trustees") shouldBe true
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
      headers(result).get("Location").get.contains("/add-subsidiary-company") shouldBe true
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
      headers(result).get("Location").get.contains("/annual-return-summary") shouldBe true
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
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display error page if fetch request object fails" in {
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(mock[CompanyDetailsList]))
      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display group plan summary if showGroupPlanSummaryPage is called with authentication and correct cache" in {
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))

      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))


      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest)

      status(result) shouldBe OK
      contentAsString(result).contains(Messages("ers_group_summary.csop.title"))
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
      headers(result).get("Location").get.contains("/alterations-or-a-variation")
    }

    "redirect to alterations page for SAYE" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SAYE)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/alterations-or-a-variation")
    }

    "redirect to summary page for EMI" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_EMI)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/summary")
    }

    "redirect to trustee page for SIP" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SIP)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/trustee-details")
    }

    "redirect to summary page for OTHER" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_OTHER)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/summary")
    }
  }
}
