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

import java.util.NoSuchElementException

import akka.stream.Materializer
import config.ApplicationConfig
import metrics.Metrics
import models.{ErsMetaData, _}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.http.Status
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Request
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.SessionService
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.test.UnitSpec
import utils.ContentUtil._
import utils.{AuthHelper, CacheUtil, ERSFakeApplicationConfig, Fixtures}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import utils.Fixtures.ersRequestObject


class ReturnServiceControllerTest extends UnitSpec with ERSFakeApplicationConfig with AuthHelper with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer
  implicit val request: Request[_] = FakeRequest()

  lazy val mockHttp: HttpPost = mock[HttpPost]
  lazy val mockHttpGet: HttpGet = mock[HttpGet]
  lazy val mockSessionCache: SessionService = mock[SessionService]
  lazy val ExpectedRedirectionUrlIfNotSignedIn = "/gg/sign-in?continue=/submit-your-ers-return"
  lazy val schemeInfo = SchemeInfo("XA1100000000000", DateTime.now, "1", "2016", "EMI", "EMI")
  lazy val rsc: ErsMetaData = new ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

  def buildFakeReturnServiceController(accessThresholdValue: Int = 100) = new ReturnServiceController {
		override val authConnector: PlayAuthConnector = mockAuthConnector

    val accessDeniedUrl = "/denied.html"
    var fetchMapVal = "e"


    when(
      mockHttp.POST[ValidatorData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(
      Future.successful(HttpResponse(OK))
    )

    override val cacheUtil: CacheUtil = new CacheUtil {
      override val sessionService: SessionService = mockSessionCache

      override def cache[T](key: String, body: T)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]) = {
        Future.successful(CacheMap("1", Map(key -> JsString("result"))))
      }

      override def cache[T](key: String, body: T, ref: String)(implicit hc:HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]) = {
        Future.successful(CacheMap("1", Map(key -> JsString("result"))))
      }

      @throws(classOf[NoSuchElementException])
      override def fetch[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[T] = {

        fetchMapVal match {
          case "e" =>
            Future(throw new NoSuchElementException)

          case "withSchemeType" =>
            Future.successful(rsc.asInstanceOf[T])

          case "withZeroErrorCount" =>
            Future.successful(rsc.asInstanceOf[T])
          case "withErrorCountSchemeTypeFileNameFileType" =>
            Future.successful(rsc.asInstanceOf[T])

          case "withSchemeAndFileType" =>
            Future.successful(rsc.asInstanceOf[T])

          case "withMatchingSchemeRef" =>
            Future.successful(rsc.asInstanceOf[T])

          case "withNonMatchingSchemeRef" =>
            Future.successful(rsc.asInstanceOf[T])
        }
      }

      override def shortLivedCache: ShortLivedCache = mock[ShortLivedCache]
    }
    override val accessThreshold: Int = accessThresholdValue
    override val metrics: Metrics = mock[Metrics]
  }

  val schemeRef: String = Fixtures.schemeRef


  "Calling ReturnServiceController.cacheParams with existing cache storage for the given schemeId and schemeRef" should {
    "retrieve the stored cache and redirect to the initial start page" in {
      val controllerUnderTest = buildFakeReturnServiceController()
      controllerUnderTest.fetchMapVal = "withMatchingSchemeRef"

      val result = controllerUnderTest.cacheParams(ersRequestObject)(Fixtures.buildFakeRequestWithSessionIdCSOP("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("h1").text() should include(Messages("ers_start.page_title", ersRequestObject.getSchemeName))
    }
  }

  "Calling ReturnServiceController.cacheParams with no matching cache storage for the given schemeId and schemeRef" should {
    "create a new cache object and redirect to the initial start page" in {
      val controllerUnderTest = buildFakeReturnServiceController()
      controllerUnderTest.fetchMapVal = "withNonMatchingSchemeRef"

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
      Helpers.redirectLocation(result).get.startsWith(ApplicationConfig.ggSignInUrl) shouldBe true
    }
  }

  "Calling ReturnServiceController.startPage" should {
    "without authentication should redirect to to company authentication frontend" in {
			setUnauthorisedMocks()
      implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("?")
      val controllerUnderTest = buildFakeReturnServiceController(accessThresholdValue = 0)
      val result = await(controllerUnderTest.startPage()(fakeRequest))
      Helpers.redirectLocation(result).get.startsWith(ApplicationConfig.ggSignInUrl) shouldBe true
    }
  }

}
