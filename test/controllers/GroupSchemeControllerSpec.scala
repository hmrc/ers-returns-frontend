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

import akka.stream.Materializer
import controllers.subsidiaries.GroupSchemeController
import org.apache.pekko.stream.Materializer
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, group, group_plan_summary}

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

  implicit lazy val materializer: Materializer         = app.materializer
  val globalErrorView: global_error                    = app.injector.instanceOf[global_error]
  val groupView: group                                 = app.injector.instanceOf[group]
  val groupPlanSummaryView: group_plan_summary         = app.injector.instanceOf[group_plan_summary]

  val company: CompanyDetails                     = {
    CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, None, None, None, None,false)
  }

  val company1: CompanyDetails                     = {
    CompanyDetails(Fixtures.companyName, "Address Line 1", Some("Address line 2"), None, None, None, None, None, None,true)
  }
  lazy val companyDetailsList: CompanyDetailsList = CompanyDetailsList(List(company, company1))

  lazy val emptyComapnyDetailsList: CompanyDetailsList = CompanyDetailsList(List.empty)

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
    mockAuthConnector,
    mockCountryCodes,
    mockErsUtil,
    mockSessionService,
    mockAppConfig,
    globalErrorView,
    groupView,
    groupPlanSummaryView,
    testAuthAction
  )

  "manualCompanyDetailsPage" should {

    def manualCompanyDetailsPageHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(
      handler: Future[Result] => Any
    ): Unit =
      handler(testGroupSchemeController.groupSchemePage().apply(request))

    "redirect to sign in page if user is not authenticated" in {
      setUnauthorisedMocks()
      manualCompanyDetailsPageHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result)                                                  shouldBe SEE_OTHER
        headers(result)(implicitly)("Location").contains("/gg/sign-in") shouldBe true
      }
    }
  }


    lazy val controllerUnderTest: GroupSchemeController = new GroupSchemeController(
      mockMCC,
      mockAuthConnector,
      mockCountryCodes,
      mockErsUtil,
      mockSessionService,
      mockAppConfig,
      globalErrorView,
      groupView,
      groupPlanSummaryView,
      testAuthAction
    )
    "deleteCompany" should {
      def deleteCompanyHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type])(
        handler: Future[Result] => Any
      ): Unit =
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

      val result = testGroupSchemeController.showDeleteCompany(0)(authRequest, hc)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "give a redirect to groupPlanSummaryPage with the selected company deleted if showDeleteCompany is called with authentication and correct cache" in {

//      when(mockSessionService.fetch[CompanyDetailsList](any())(any(), any()))
//        .thenReturn(Future.successful(companyDetailsList))
//
//      when(
//        mockSessionService.fetch[RequestObject](any())(any(), any())
//      ) thenReturn Future.successful(ersRequestObject)
//
//      when(
//        mockSessionService.cache[CompanyDetailsList](any(), any())(any(), any())
//      ) thenReturn Future(sessionPair)

      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)


      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any())).thenReturn(Future(sessionPair))

      val result = testGroupSchemeController.showDeleteCompany(0)(authRequest, hc)

      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/group-summary")
    }

    "filter deleted company before caching and redirecting" in {

      when(mockSessionService.fetch[CompanyDetailsList](any())(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))

      when(
        mockSessionService.fetch[RequestObject](any())(any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      when(
        mockSessionService.cache[CompanyDetailsList](any(), any())(any(), any())
      ) thenReturn Future(sessionPair)

      when(mockErsUtil.SUBSIDIARY_COMPANIES_CACHE) thenReturn(SUBSIDIARY_COMPANIES_CACHE)

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val cacheItem = testCacheItem[CompanyDetailsList](GROUP_SCHEME_COMPANIES, companyDetailsList)
      val expected = CompanyDetailsList(List(company))
      when(mockSessionService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList]())(any(), any()))
        .thenReturn(Future(sessionPair))

      val result = testGroupSchemeController.showDeleteCompany(0)(authRequest, hc)

      status(result) shouldBe SEE_OTHER

      verify(mockSessionService, times(1)).cache(refEq(SUBSIDIARY_COMPANIES_CACHE), refEq(expected))(any(), any())
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
      val result = testGroupSchemeController.showGroupSchemePage(ersRequestObject)(authRequest, hc)

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

    "redirect to subsidiary based in uk page if user select yes for CSOP" in {
      when(
        mockSessionService.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo]())(any(), any())
      ).thenReturn(
        Future.successful(sessionPair)
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "CSOP")
      val authRequest = buildRequestWithAuth(request)

      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(authRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage().url
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
      redirectLocation(result).get shouldBe controllers.routes.AltAmendsController.altActivityPage().url
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
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage().url
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
      redirectLocation(result).get shouldBe controllers.routes.AltAmendsController.altActivityPage().url
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
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage().url
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

      redirectLocation(result).get shouldBe controllers.routes.SummaryDeclarationController.summaryDeclarationPage().url
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
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage().url
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
      redirectLocation(result).get shouldBe controllers.subsidiaries.routes.SubsidiaryBasedInUkController.questionPage().url
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

      redirectLocation(result).get shouldBe controllers.routes.SummaryDeclarationController.summaryDeclarationPage().url
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
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest, hc)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display group plan summary if showGroupPlanSummaryPage is called with authentication and correct cache" in {
      when(mockSessionService.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES))(any(), any()))
        .thenReturn(Future.successful(companyDetailsList))

      when(mockSessionService.fetch[RequestObject](refEq(mockErsUtil.ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))


      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionId("GET"))
      val result = testGroupSchemeController.showGroupPlanSummaryPage()(authRequest, hc)

      status(result) shouldBe OK
      contentAsString(result).contains(Messages("ers_group_summary.csop.title"))
    }

    "redirect to GroupSchemeController groupSchemePage if no subsidiary companies in list" in {
      setAuthMocks()

      when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(emptyComapnyDetailsList))

      when(
        mockSessionService.fetch[RequestObject](any())(any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))

      val result = controllerUnderTest.showGroupPlanSummaryPage()(authRequest, hc)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/submit-your-ers-annual-return/group-scheme")
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

    when(
      mockSessionService.fetch[RequestObject](any())(any(), any())
    ) thenReturn Future.successful(ersRequestObject)

    when(
      mockSessionService.fetchCompaniesOptionally()(any(), any())
    ).thenReturn(Future.successful(Fixtures.exampleCompanies))

    "redirect to alterations page for CSOP" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET").withFormUrlEncodedBody("addCompany" -> "1"))
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_CSOP)(authRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.AltAmendsController.altActivityPage().url
    }

    "redirect to alterations page for SAYE" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSAYE("GET").withFormUrlEncodedBody("addCompany" -> "1"))
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SAYE)(authRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.AltAmendsController.altActivityPage().url
    }

    "redirect to summary page for EMI" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdEMI("GET").withFormUrlEncodedBody("addCompany" -> "1"))
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_EMI)(authRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.SummaryDeclarationController.summaryDeclarationPage().url
    }

    "redirect to trustee page for SIP" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET").withFormUrlEncodedBody("addCompany" -> "1"))
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SIP)(authRequest)
      status(result) shouldBe SEE_OTHER
      headers(result)(implicitly)("Location").contains("/trustee-details")
      redirectLocation(result).get shouldBe controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage().url
    }

    "redirect to summary page for OTHER" in {
      val authRequest = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdOTHER("GET").withFormUrlEncodedBody("addCompany" -> "1"))
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_OTHER)(authRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.SummaryDeclarationController.summaryDeclarationPage().url
    }
  }
}
