/*
 * Copyright 2020 HM Revenue & Customs
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
import helpers.ErsTestHelper
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.Future

class GroupSchemeControllerTest extends UnitSpec with ErsTestHelper with ERSFakeApplicationConfig with BeforeAndAfterEach with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val materializer: Materializer = app.materializer
	lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val company: CompanyDetails = CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, None, None, None, None)
  lazy val companyDetailsList: CompanyDetailsList = CompanyDetailsList(List(company, company))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockErsUtil)
		when(mockErsUtil.GROUP_SCHEME_COMPANIES).thenReturn("group-scheme-companies")
		when(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER).thenReturn("group-scheme-controller")
		when(mockErsUtil.ersRequestObject).thenReturn("ErsRequestObject")
		when(mockErsUtil.OPTION_MANUAL).thenReturn("man")
		when(mockErsUtil.DEFAULT).thenReturn("")
		when(mockErsUtil.SCHEME_CSOP).thenReturn("1")
		when(mockErsUtil.SCHEME_EMI).thenReturn("2")
		when(mockErsUtil.SCHEME_OTHER).thenReturn("3")
		when(mockErsUtil.SCHEME_SAYE).thenReturn("4")
		when(mockErsUtil.SCHEME_SIP).thenReturn("5")
		when(mockErsUtil.OPTION_YES).thenReturn("1")
		when(mockErsUtil.OPTION_NO).thenReturn("2")
	}

  lazy val testGroupSchemeController: GroupSchemeController = new GroupSchemeController(messagesApi, mockAuthConnector, mockCountryCodes, mockErsUtil, mockAppConfig)

  "manualCompanyDetailsPage" should {

    def manualCompanyDetailsPageHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.manualCompanyDetailsPage(index).apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      manualCompanyDetailsPageHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showManualCompanyDetailsPage" should {
    "display company details page for correct scheme" in {
      when(mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

      val result = await(testGroupSchemeController.showManualCompanyDetailsPage(1)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET")))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers_manual_company_details.csop.title"))
    }
  }

  "manualCompanyDetailsSubmit" should {
    def manualCompanyDetailsSubmitHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.manualCompanyDetailsSubmit(index).apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      manualCompanyDetailsSubmitHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showManualCompanyDetailsSubmit" should {

    def buildCompanyDetailsRequest(isValid: Boolean) = {
      val data = if (isValid) {
        Map("companyName" -> Fixtures.companyName,
					"addressLine1" -> "Add1",
					"addressLine2" -> "Add2",
					"addressLine3" -> "Add3",
					"addressLine4" -> "Add4",
					"postcode" -> "AA111AA",
					"country" -> "United Kingdom",
					"companyReg" -> "",
					"corporationRef" -> ""
				)
      }
      else {
        Map("" -> "")
      }
      val form = _root_.models.RsFormMappings.companyDetailsForm.bind(data)
      Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
    }

    "display error if showManualCompanyDetailsSubmit is called with authentication and form errors" in {
      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1000)(Fixtures.buildFakeUser, buildCompanyDetailsRequest(isValid = false))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("validation.summary.heading")) shouldBe true
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data entered for 1st company" in {
      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(companyDetailsList)
      )
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 10000)(Fixtures.buildFakeUser, buildCompanyDetailsRequest(isValid = true))
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/group-summary")
    }


    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data for additional company" in {
      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(companyDetailsList)
      )
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1)(Fixtures.buildFakeUser, buildCompanyDetailsRequest(isValid = true))
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/group-summary")
    }

    "redirect to Group Summary page if showManualCompanyDetailsSubmit is called with authentication and correct form data for updated company" in {
      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(companyDetailsList)
      )
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 0)(Fixtures.buildFakeUser, buildCompanyDetailsRequest(isValid = true))
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/group-summary")
    }


    "redirect to Group Summary page if data is filled correctly and there is nothing in existing cache" in {
      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new NoSuchElementException("Nothing in cache"))
      )
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val result = testGroupSchemeController.showManualCompanyDetailsSubmit(ersRequestObject, 1000)(Fixtures.buildFakeUser, buildCompanyDetailsRequest(isValid = true))
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/group-summary")
    }
  }

  "calling replace company" should {

		lazy val controllerUnderTest: GroupSchemeController = new GroupSchemeController(messagesApi, mockAuthConnector, mockCountryCodes, mockErsUtil, mockAppConfig)

    "replace a companies and keep the other companies" when {

      "given an index that matches a companies in the list" in {

        val index = 2

        val formData = CompanyDetails("Replacement Company", "1 Some Place", None, None, None, None, None, None, None)
        val target = CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None)

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
        val target = CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None)

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
    def deleteCompanyHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.deleteCompany(index).apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
      deleteCompanyHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showDeleteCompany" should {
    "direct to ers errors page if fetchAll fails" in {

      when(
        mockErsUtil.fetchAll(anyString())(any(), any())
      ) thenReturn Future.failed(new NoSuchElementException("Nothing in cache"))

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val result = await(testGroupSchemeController.showDeleteCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "direct to ers errors page if fetch request object fails" in {

      when(
        mockErsUtil.fetchAll(anyString())(any(), any())
      ) thenReturn Future.successful(mock[CacheMap])

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.failed(new Exception)

      val result = await(testGroupSchemeController.showDeleteCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "give a redirect to groupPlanSummaryPage with the selected company deleted if showDeleteCompany is called with authentication and correct cache" in {

      when(mockErsUtil.fetchAll(anyString())(any(), any()))
				.thenReturn(Future.successful(CacheMap("id1", Map(GROUP_SCHEME_COMPANIES -> Json.toJson(companyDetailsList)))))

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList](), anyString())(any(), any(), any())
      ) thenReturn mock[CacheMap]


      val result = await(testGroupSchemeController.showDeleteCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/group-summary")
    }

    "filter deleted company before caching and redirecting" in {

      when(
        mockErsUtil.fetchAll(anyString())(any(), any())
      ).thenReturn(
        Future.successful(
          CacheMap("id1", Map(GROUP_SCHEME_COMPANIES -> Json.toJson(companyDetailsList)))
        )
      )

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), any[CompanyDetailsList](), anyString())(any(), any(), any())
      ) thenReturn mock[CacheMap]

      val expected = CompanyDetailsList(List(company))

      val result = await(testGroupSchemeController.showDeleteCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe SEE_OTHER

      verify(mockErsUtil, times(1)).cache(refEq(GROUP_SCHEME_COMPANIES), refEq(expected), refEq(ersRequestObject.getSchemeReference))(any(), any(), any())
    }
  }

  "editCompany" should {
    def editCompanyHandler(index: Int, request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.editCompany(index).apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      editCompanyHandler(0, Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showEditCompany" should {
    "display error page if fetch company details list fails" in {

      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ) thenReturn Future.failed(new NoSuchElementException("Nothing in cache"))

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val result = await(testGroupSchemeController.showEditCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display error page if fetch request object fails" in {

      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ) thenReturn Future.successful(companyDetailsList)

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.failed(new Exception)

      val result = await(testGroupSchemeController.showEditCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display manualCompanyDetailsPage with the selected company details if showEditCompany is called with authentication and correct cache" in {

      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ) thenReturn Future.successful(companyDetailsList)

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val result = await(testGroupSchemeController.showEditCompany(0)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers_manual_company_details.csop.title"))
    }

  }

  "groupSchemePage" should {
    def groupSchemePageHandler(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.groupSchemePage().apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      groupSchemePageHandler(Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showGroupSchemePage" should {
    "display group scheme page if there is no data in cache" in {
      when(
        mockErsUtil.fetch[GroupSchemeInfo](refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), anyString())(any(), any(), any())
      ).thenReturn(
        Future.failed(new NoSuchElementException("Nothing in cache"))
      )
      val result = await(testGroupSchemeController.showGroupSchemePage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=yes]").hasAttr("checked") shouldEqual false
      document.select("input[id=no]").hasAttr("checked") shouldEqual false
    }
    "display group scheme page if there is cached data" in {
      when(
        mockErsUtil.fetch[GroupSchemeInfo](refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), anyString())(any(), any(), any())
      ).thenReturn(
        Future.successful(GroupSchemeInfo(Option(OPTION_YES), None))
      )
      val result = await(testGroupSchemeController.showGroupSchemePage(ersRequestObject)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=yes]").hasAttr("checked") shouldEqual true
      document.select("input[id=no]").hasAttr("checked") shouldEqual false
    }
  }

  "groupSchemeSelected" should {
    def groupSchemeSelectedHandler(scheme: String, request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.groupSchemeSelected(scheme).apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      groupSchemeSelectedHandler("", Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showGroupSchemeSelected" should {
    def buildGroupSchemeSelectedRequest(result: Option[Boolean] = None, scheme: String) = {
      val data = result match {
        case None => Map("" -> "")
        case Some(true) => Map("groupScheme" -> OPTION_YES)
        case Some(false) => Map("groupScheme" -> OPTION_NO)
      }
      val form = _root_.models.RsFormMappings.groupForm.bind(data)
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
      val request = buildGroupSchemeSelectedRequest(None, "CSOP")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(Fixtures.buildFakeUser, request)
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("validation.summary.heading")) shouldBe true
    }

    "display errors if no data is set" in {
      val request = buildGroupSchemeSelectedRequest(None, "")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(Fixtures.buildFakeUser, request)
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("validation.summary.heading")) shouldBe true
    }

    "redirect to company details page if user select yes for CSOP" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "CSOP")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/add-company-details-manually")
    }

    "redirect to alterations page if user select no for CSOP" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "CSOP")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, SCHEME_CSOP)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/alterations")
    }

    "redirect to company details page if user select yes for SAYE" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "SAYE")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_SAYE)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/add-company-details-manually")
    }

    "redirect to alterations page if user select no for SAYE" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "SAYE")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_SAYE)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/alterations")
    }

    "redirect to company details page if user select yes for EMI" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "EMI")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_EMI)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/add-company-details-manually")
    }

    "redirect to summary page if user select no for EMI" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "EMI")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_EMI)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/summary")
    }

    "redirect to company details page if user select yes for SIP" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "SIP")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_SIP)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/add-company-details-manually")
    }

    "redirect to trustees page if user select no for SIP" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "SIP")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_SIP)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/trustee-details")
    }

    "redirect to company details page if user select yes for OTHER" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(true), "OTHER")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_OTHER)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/add-company-details-manually")
    }

    "redirect to summary page if user select no for OTHER" in {
      when(
        mockErsUtil.cache(refEq(mockErsUtil.GROUP_SCHEME_CACHE_CONTROLLER), any[GroupSchemeInfo](), anyString())(any(), any(), any())
      ).thenReturn(
        mock[CacheMap]
      )
      val request = buildGroupSchemeSelectedRequest(Some(false), "OTHER")
      val result = testGroupSchemeController.showGroupSchemeSelected(ersRequestObject, mockErsUtil.SCHEME_OTHER)(Fixtures.buildFakeUser, request)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/summary")
    }

  }

  "groupPlanSummaryPage" should {
    def groupPlanSummaryPageHandler(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.groupPlanSummaryPage().apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      groupPlanSummaryPageHandler(Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "showGroupPlanSummaryPage" should {

    "display error page if fetch company details list fails" in {

      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ) thenReturn Future.failed(new NoSuchElementException("Nothing in cache"))

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val result = await(testGroupSchemeController.showGroupPlanSummaryPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display error page if fetch request object fails" in {

      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ) thenReturn Future.successful(mock[CompanyDetailsList])

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.failed(new Exception)

      val result = await(testGroupSchemeController.showGroupPlanSummaryPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers.global_errors.title")) shouldBe true
    }

    "display group plan summary if showGroupPlanSummaryPage is called with authentication and correct cache" in {
      when(
        mockErsUtil.fetch[CompanyDetailsList](refEq(mockErsUtil.GROUP_SCHEME_COMPANIES), anyString())(any(), any(), any())
      ) thenReturn Future.successful(companyDetailsList)

      when(
        mockErsUtil.fetch[RequestObject](refEq(mockErsUtil.ersRequestObject))(any(), any(), any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val result = await(testGroupSchemeController.showGroupPlanSummaryPage()(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc))
      status(result) shouldBe OK
      bodyOf(result).contains(Messages("ers_group_summary.csop.title"))
    }
  }

  "groupPlanSummaryContinue" should {
    def groupPlanSummaryContinueHandler(scheme: String, request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest())(handler: Future[Result] => Any): Unit = {
      handler(testGroupSchemeController.groupPlanSummaryContinue(scheme).apply(request))
    }

    "redirect to sign in page if user is not authenticated" in {
			setUnauthorisedMocks()
      groupPlanSummaryContinueHandler("", Fixtures.buildFakeRequestWithSessionId("GET")) { result =>
        status(result) shouldBe SEE_OTHER
        headers(result).get("Location").get.contains("/gg/sign-in") shouldBe true
      }
    }
  }

  "continueFromGroupPlanSummaryPage" should {
    "redirect to alterations page for CSOP" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_CSOP)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/alterations")
    }

    "redirect to alterations page for SAYE" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SAYE)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/alterations")
    }

    "redirect to summary page for EMI" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_EMI)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/summary")
    }

    "redirect to trustee page for SIP" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_SIP)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/trustee-details")
    }

    "redirect to summary page for OTHER" in {
      val result = testGroupSchemeController.continueFromGroupPlanSummaryPage(mockErsUtil.SCHEME_OTHER)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe SEE_OTHER
      headers(result).get("Location").get.contains("/summary")
    }
  }
}
