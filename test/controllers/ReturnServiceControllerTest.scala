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
import models.{ErsMetaData, _}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.http.Status
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsString
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.Future


class ReturnServiceControllerTest extends UnitSpec with ERSFakeApplicationConfig with ErsTestHelper with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
	lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
	implicit lazy val mat: Materializer = app.materializer
	val hundred = 100

  lazy val ExpectedRedirectionUrlIfNotSignedIn = "/gg/sign-in?continue=/submit-your-ers-return"
  lazy val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")
  lazy val rsc: ErsMetaData = new ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))
	lazy val rscAsRequestObject: RequestObject = RequestObject(Some("aoRef"), Some("2014/15"), Some("AA0000000000000"), Some("MyScheme"),
			Some("CSOP"), Some("agentRef"), Some("empRef"), Some("ts"), Some("hmac"))

  def buildFakeReturnServiceController(accessThresholdValue: Int = hundred): ReturnServiceController =
		new ReturnServiceController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig) {

		override lazy val accessThreshold: Int = accessThresholdValue
    override val accessDeniedUrl: String = "/denied.html"
		val cacheResponse: Future[CacheMap] = Future.successful(CacheMap("1", Map("key" -> JsString("result"))))

    when(mockHttp.POST[ValidatorData, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
			.thenReturn(Future.successful(HttpResponse(OK)))
		when(mockErsUtil.cache(any(), any())(any(), any(), any(), any())).thenReturn(cacheResponse)
		when(mockErsUtil.cache(any(), any(),any())(any(), any(), any())).thenReturn(cacheResponse)
		when(mockErsUtil.fetch[RequestObject](any(), any())(any(), any(), any())).thenReturn(Future.successful(rscAsRequestObject))
  }

  "Calling ReturnServiceController.cacheParams with existing cache storage for the given schemeId and schemeRef" should {
    "retrieve the stored cache and redirect to the initial start page" in {
      val controllerUnderTest = buildFakeReturnServiceController()

      val result = controllerUnderTest.cacheParams(ersRequestObject)(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h1").text() should include(Messages("ers_start.page_title", ersRequestObject.getSchemeName))
    }
  }

  "Calling ReturnServiceController.cacheParams with no matching cache storage for the given schemeId and schemeRef" should {
    "create a new cache object and redirect to the initial start page" in {
      val controllerUnderTest = buildFakeReturnServiceController()
      val result = controllerUnderTest.cacheParams(ersRequestObject)(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe Status.OK
    }
  }

  //Start Page
  "Calling ReturnServiceController.startPage (GET) without authentication" should {
    "give a redirect status (to company authentication frontend)" in {
			setUnauthorisedMocks()
      val controllerUnderTest = buildFakeReturnServiceController()
      val result = controllerUnderTest.startPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling ReturnServiceController.hmacCheck" should {
    "without authentication should redirect to to company authentication frontend" in {
			setUnauthorisedMocks()
      implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("?")
      val controllerUnderTest = buildFakeReturnServiceController(accessThresholdValue = 0)
      val result = await(controllerUnderTest.hmacCheck()(fakeRequest))
      Helpers.redirectLocation(result).get.startsWith(mockAppConfig.ggSignInUrl) shouldBe true
    }
  }

  "Calling ReturnServiceController.startPage" should {
    "without authentication should redirect to to company authentication frontend" in {
			setUnauthorisedMocks()
      implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("?")
      val controllerUnderTest = buildFakeReturnServiceController(accessThresholdValue = 0)
      val result = await(controllerUnderTest.startPage()(fakeRequest))
      Helpers.redirectLocation(result).get.startsWith(mockAppConfig.ggSignInUrl) shouldBe true
    }
  }

}
