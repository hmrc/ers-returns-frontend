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

import akka.stream.Materializer
import models.{ErsMetaData, _}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.libs.json.JsString
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, start, unauthorised}

import scala.concurrent.{ExecutionContext, Future}

class ReturnServiceControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ERSFakeApplicationConfig
    with ErsTestHelper
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

  implicit lazy val mat: Materializer = app.materializer
  val globalErrorView: global_error   = app.injector.instanceOf[global_error]
  val unauthorisedView: unauthorised  = app.injector.instanceOf[unauthorised]
  val startView: start                = app.injector.instanceOf[start]
  val hundred                         = 100

  lazy val ExpectedRedirectionUrlIfNotSignedIn = "/gg/sign-in?continue=/submit-your-ers-return"
  lazy val schemeInfo: SchemeInfo              = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")
  lazy val rsc: ErsMetaData                    =
    new ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
  lazy val rscAsRequestObject: RequestObject   = RequestObject(
    Some("aoRef"),
    Some("2014/15"),
    Some("AA0000000000000"),
    Some("MyScheme"),
    Some("CSOP"),
    Some("agentRef"),
    Some("empRef"),
    Some("ts"),
    Some("hmac")
  )

  def buildFakeReturnServiceController(accessThresholdValue: Int = hundred): ReturnServiceController =
    new ReturnServiceController(
      mockMCC,
      mockAuthConnector,
      mockErsUtil,
      mockAppConfig,
      globalErrorView,
      unauthorisedView,
      startView,
      testAuthActionGov
    ) {

      override lazy val accessThreshold: Int = accessThresholdValue
      override val accessDeniedUrl: String   = "/denied.html"
      val cacheResponse: Future[CacheMap]    = Future.successful(CacheMap("1", Map("key" -> JsString("result"))))

    when(mockHttp.POST[ValidatorData, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
			.thenReturn(Future.successful(HttpResponse(OK, "")))
		when(mockErsUtil.cache(any(), any())(any(), any(), any())).thenReturn(cacheResponse)
		when(mockErsUtil.cache(any(), any(),any())(any(), any())).thenReturn(cacheResponse)
		when(mockErsUtil.fetch[RequestObject](any(), any())(any(), any())).thenReturn(Future.successful(rscAsRequestObject))
  }

  "Calling ReturnServiceController.cacheParams with existing cache storage for the given schemeId and schemeRef" should {
    "retrieve the stored cache and redirect to the initial start page" in {
      val controllerUnderTest = buildFakeReturnServiceController()

      val result =
        controllerUnderTest.cacheParams(ersRequestObject)(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h1").text() should include(
        Messages("ers_start.page_title", ersRequestObject.getSchemeName)
      )
    }
  }

  "Calling ReturnServiceController.cacheParams with no matching cache storage for the given schemeId and schemeRef" should {
    "create a new cache object and redirect to the initial start page" in {
      val controllerUnderTest = buildFakeReturnServiceController()
      val result              =
        controllerUnderTest.cacheParams(ersRequestObject)(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }

  //Start Page
  "Calling ReturnServiceController.startPage (GET) without authentication" should {
    "give a redirect status (to company authentication frontend)" in {
      setUnauthorisedMocks()
      val controllerUnderTest = buildFakeReturnServiceController()
      val result              = controllerUnderTest.startPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling ReturnServiceController.hmacCheck" should {
    "without authentication should redirect to to company authentication frontend" in {
      setUnauthorisedMocks()
      implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("?")
      val controllerUnderTest  = buildFakeReturnServiceController(accessThresholdValue = 0)
      val result               = controllerUnderTest.hmacCheck()(fakeRequest)
      Helpers.redirectLocation(result).get.startsWith(mockAppConfig.ggSignInUrl) shouldBe true
    }
  }

  "Calling ReturnServiceController.startPage" should {
    "without authentication should redirect to to company authentication frontend" in {
      setUnauthorisedMocks()
      implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("?")
      val controllerUnderTest  = buildFakeReturnServiceController(accessThresholdValue = 0)
      val result               = controllerUnderTest.startPage()(fakeRequest)
      Helpers.redirectLocation(result).get.startsWith(mockAppConfig.ggSignInUrl) shouldBe true
    }
  }

}
