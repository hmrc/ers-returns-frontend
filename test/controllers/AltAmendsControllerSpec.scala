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

package controllers

import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{
  AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents
}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Fixtures.ersRequestObject
import utils._
import views.html.{alterations_activity, alterations_amends, global_error}

import scala.concurrent.{ExecutionContext, Future}

class AltAmendsControllerSpec
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

  val fakeGroupSchemeInfo: GroupSchemeInfo     = GroupSchemeInfo(Some(OPTION_NO), Some(""))
  val fakeAltAmendsActivity: AltAmendsActivity = AltAmendsActivity("1")

  "calling Alterations Activity Page" should {

    def buildFakeAltAmendsPageController(
      groupSchemeActivity: Future[GroupSchemeInfo] = Future.successful(fakeGroupSchemeInfo),
      altAmendsActivity: Future[AltAmendsActivity] = Future.successful(fakeAltAmendsActivity),
      successfulCache: Boolean = true,
      requestObject: Future[RequestObject] = Future.successful(ersRequestObject),
      alterationsActivityView: alterations_activity = app.injector.instanceOf[alterations_activity],
      alterationsAmendsView: alterations_amends = app.injector.instanceOf[alterations_amends],
      globalErrorView: global_error = app.injector.instanceOf[global_error]
    ): AltAmendsController =
      new AltAmendsController(
        mockMCC,
        mockSessionService,
        alterationsActivityView,
        alterationsAmendsView,
        globalErrorView,
        testAuthAction
      ) {
        when(mockSessionService.fetch[GroupSchemeInfo](ArgumentMatchers.eq("group-scheme-controller"))(any(), any()))
          .thenReturn(groupSchemeActivity)

        when(mockSessionService.fetch[AltAmendsActivity](ArgumentMatchers.eq("alt-activity"))(any(), any()))
          .thenReturn(altAmendsActivity)

        when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any())).thenReturn(requestObject)

        when(mockSessionService.cache(ArgumentMatchers.eq("alt-activity"), any())(any(), any()))
          .thenReturn(if (successfulCache) Future.successful(sessionPair) else Future.failed(new Exception("error")))

        when(mockErsUtil.getPageElement(any(), any(), any(), any())(any())).thenCallRealMethod()
      }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val result                                   = controllerUnderTest.altActivityPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeAltAmendsPageController()
      val result              = controllerUnderTest.altActivityPage().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetching groupSchemeActivity throws exception" in {
      val controllerUnderTest = buildFakeAltAmendsPageController(groupSchemeActivity = Future.failed(new Exception))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)
      val result              = controllerUnderTest.showAltActivityPage()(authRequest)

      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeAltAmendsPageController().getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "direct to ers errors page if fetching requestObject throws exception" in {
      val controllerUnderTest = buildFakeAltAmendsPageController(requestObject = Future.failed(new Exception))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)
      val result              = controllerUnderTest.showAltActivityPage()(authRequest)

      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeAltAmendsPageController().getGlobalErrorPage(testFakeRequest, testMessages))
      )
    }

    "show alterations activity page with selection if fetching altAmendsActivity successful" in {
      val controllerUnderTest = buildFakeAltAmendsPageController()
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltActivityPage()(authRequest)

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=no]").hasAttr("checked") shouldEqual false
      document.select("input[id=yes]").hasAttr("checked") shouldEqual true
    }

    "show alterations activity page with nothing selected if fetching altAmendsActivity fails" in {
      val controllerUnderTest = buildFakeAltAmendsPageController(altAmendsActivity = Future.failed(new Exception))
      val authRequest         = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltActivityPage()(authRequest)

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=no]").hasAttr("checked") shouldEqual false
      document.select("input[id=yes]").hasAttr("checked") shouldEqual false
    }

    "give a redirect status (to company authentication frontend) on POST if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeAltAmendsPageController()

      val result = controllerUnderTest.altActivitySelected()(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on POST if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest = buildFakeAltAmendsPageController()

      val result = controllerUnderTest.altActivitySelected()(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      status(result) shouldBe Status.OK
    }

    "give a Ok status and stay on the same page if form errors and display the error" in {
      val controllerUnderTest = buildFakeAltAmendsPageController()
      val altActivityData     = Map("" -> "")
      val form                = RsFormMappings.altActivityForm().bind(altActivityData)
      val request             = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request)

      val result = controllerUnderTest.showAltActivitySelected()(authRequest)

      status(result) shouldBe Status.OK
    }

    "give a redirect to summary declaration page if no form errors and NO selected" in {
      val controllerUnderTest           = buildFakeAltAmendsPageController()
      val altActivityData               = Map("altActivity" -> OPTION_NO)
      val form: Form[AltAmendsActivity] = RsFormMappings.altActivityForm().bind(altActivityData)
      val request                       = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest                   = buildRequestWithAuth(request, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltActivitySelected()(authRequest)

      status(result)         shouldBe Status.SEE_OTHER
      result.futureValue.header
        .headers("Location") shouldBe routes.SummaryDeclarationController.summaryDeclarationPage().toString
    }

    "give a redirect to alterations amends page if no form errors and YES selected" in {
      val controllerUnderTest = buildFakeAltAmendsPageController()
      val altActivityData     = Map("altActivity" -> OPTION_YES)
      val form                = RsFormMappings.altActivityForm().bind(altActivityData)
      val request             = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(request, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltActivitySelected()(authRequest)

      status(result)                                shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.AltAmendsController.altAmendsPage().toString
    }

    "direct to ers errors page if no form errors but unable to save to cache" in {
      val controllerUnderTest = buildFakeAltAmendsPageController(successfulCache = false)
      val altActivityData     = Map("altActivity" -> OPTION_YES)
      val form                = RsFormMappings.altActivityForm().bind(altActivityData)
      val req                 = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(req, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltActivitySelected()(authRequest)

      status(result)          shouldBe Status.OK
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeAltAmendsPageController().getGlobalErrorPage(req, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "direct to ers errors page if no form errors and no sessionId" in {
      val controllerUnderTest = buildFakeAltAmendsPageController(successfulCache = false)
      val altActivityData     = Map("altActivity" -> OPTION_YES)
      val form                = RsFormMappings.altActivityForm().bind(altActivityData)
      val req                 = Fixtures.buildFakeRequest("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest         = buildRequestWithAuth(req, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltActivitySelected()(authRequest)

      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeAltAmendsPageController().getGlobalErrorPage(req, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }
  }

  "calling Alterations Amends Page" should {

    val altAmends = Future.successful(Some(AltAmends(Some("0"), Some("1"), Some("1"), Some("1"), Some("0"))))

    def buildFakeAltAmendsPageController(
      altAmends: Future[Option[AltAmends]] = altAmends,
      requestObject: Future[RequestObject] = Future.successful(ersRequestObject),
      successfulCache: Boolean = true,
      alterationsActivityView: alterations_activity = app.injector.instanceOf[alterations_activity],
      alterationsAmendsView: alterations_amends = app.injector.instanceOf[alterations_amends],
      globalErrorView: global_error = app.injector.instanceOf[global_error]
    ): AltAmendsController = new AltAmendsController(
      mockMCC,
      mockSessionService,
      alterationsActivityView,
      alterationsAmendsView,
      globalErrorView,
      testAuthAction
    ) {

      when(mockSessionService.fetch[RequestObject](refEq(ERS_REQUEST_OBJECT))(any(), any()))
        .thenReturn(requestObject)
      when(mockSessionService.fetchOption[AltAmends](refEq("alt-amends-cache-controller"), any())(any(), any()))
        .thenReturn(altAmends)
      when(mockSessionService.cache(refEq("alt-amends-cache-controller"), any())(any(), any()))
        .thenReturn(if (successfulCache) Future.successful(sessionPair) else Future.failed(new Exception("error")))
      when(mockErsUtil.getPageElement(any(), any(), any(), any())(any())).thenReturn("")
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val result                                   = controllerUnderTest.altAmendsPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status OK on GET if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val result                                   = controllerUnderTest.altAmendsPage().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))
      status(result) shouldBe Status.OK
    }

    "shows alterations amends page with no selection if fetching altAmends fails" in {
      val controllerUnderTest: AltAmendsController =
        buildFakeAltAmendsPageController(altAmends = Future.failed(new Exception))
      val authRequest                              = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsPage()(authRequest)

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=alt-terms-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=alt-eligibility-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=alt-exchange-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=variations-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=other-check-box]").hasAttr("checked") shouldEqual false
    }

    "shows alterations amends page with no selection if fetching altAmends returns None" in {
      val controllerUnderTest: AltAmendsController =
        buildFakeAltAmendsPageController(altAmends = Future.successful(None))
      val authRequest                              = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsPage()(authRequest)

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=alt-terms-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=alt-eligibility-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=alt-exchange-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=variations-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=other-check-box]").hasAttr("checked") shouldEqual false
    }

    "show alterations amends page with selection if fetching altAmends successful" in {
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val authRequest                              = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsPage()(authRequest)

      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=alt-terms-check-box]").hasAttr("checked") shouldEqual false
      document.select("input[id=alt-eligibility-check-box]").hasAttr("checked") shouldEqual true
      document.select("input[id=alt-exchange-check-box]").hasAttr("checked") shouldEqual true
      document.select("input[id=variations-check-box]").hasAttr("checked") shouldEqual true
      document.select("input[id=other-check-box]").hasAttr("checked") shouldEqual false
    }

    "give a redirect status (to company authentication frontend) on POST if user is not authenticated" in {
      setUnauthorisedMocks()
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val result                                   = controllerUnderTest.altAmendsSelected().apply(FakeRequest("GET", ""))

      status(result) shouldBe Status.SEE_OTHER
    }

    "give a status SEE_OTHER on POST if user is authenticated" in {
      setAuthMocks()
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()

      val result = controllerUnderTest.altAmendsSelected().apply(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"))

      status(result) shouldBe Status.SEE_OTHER
    }

    "give a redirect status and stay on the same page if nothing selected" in {
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val altAmendsData                            = Map(
        "altAmendsTerms"       -> "",
        "altAmendsEligibility" -> "",
        "altAmendsExchange"    -> "",
        "altAmendsVariations"  -> "",
        "altAmendsOther"       -> ""
      )
      val form                                     = RsFormMappings.altAmendsForm().bind(altAmendsData)
      val request                                  = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest                              = buildRequestWithAuth(request, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsSelected()(authRequest)
      status(result)                                shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.AltAmendsController.altAmendsPage().toString
    }

    "give a redirect status and stay on the same page if form errors" in {
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val altAmendsData                            = Map(
        "altAmendsTerms"       -> "X",
        "altAmendsEligibility" -> "",
        "altAmendsExchange"    -> "",
        "altAmendsVariations"  -> "",
        "altAmendsOther"       -> ""
      )
      val form                                     = RsFormMappings.altAmendsForm().bind(altAmendsData)
      val request                                  = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest                              = buildRequestWithAuth(request, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsSelected()(authRequest)

      status(result)                                shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.AltAmendsController.altAmendsPage().toString
    }

    "direct to ers errors page if no form errors but unable to save to cache" in {
      val controllerUnderTest: AltAmendsController =
        buildFakeAltAmendsPageController(successfulCache = false)
      val altAmendsData                            = Map(
        "altAmendsTerms"       -> "1",
        "altAmendsEligibility" -> "1",
        "altAmendsExchange"    -> "1",
        "altAmendsVariations"  -> "1",
        "altAmendsOther"       -> "1"
      )
      val form                                     = RsFormMappings.altActivityForm().bind(altAmendsData)
      val req                                      = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest                              = buildRequestWithAuth(req, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsSelected()(authRequest)

      status(result)          shouldBe Status.OK
      contentAsString(result) shouldBe contentAsString(
        Future(buildFakeAltAmendsPageController().getGlobalErrorPage(req, testMessages))
      )
      contentAsString(result)   should include(testMessages("ers.global_errors.message"))
    }

    "give a redirect to summary declaration page if no form errors and selection applied" in {
      val controllerUnderTest: AltAmendsController = buildFakeAltAmendsPageController()
      val altAmendsData                            = Map(
        "altAmendsTerms"       -> "1",
        "altAmendsEligibility" -> "1",
        "altAmendsExchange"    -> "1",
        "altAmendsVariations"  -> "1",
        "altAmendsOther"       -> "1"
      )
      val form                                     = RsFormMappings.altActivityForm().bind(altAmendsData)
      val request                                  = Fixtures.buildFakeRequestWithSessionIdCSOP("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val authRequest                              = buildRequestWithAuth(request, Fixtures.buildFakeUser)

      val result = controllerUnderTest.showAltAmendsSelected()(authRequest)

      status(result)         shouldBe Status.SEE_OTHER
      result.futureValue.header
        .headers("Location") shouldBe routes.SummaryDeclarationController.summaryDeclarationPage().toString
    }
  }

}
