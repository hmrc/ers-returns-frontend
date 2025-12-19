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

import controllers.auth.{AuthActionGovGateway, RequestWithOptionalAuthContext}
import models.{ErsMetaData, SchemeInfo}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.UNAUTHORIZED
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ErsTestHelper
import views.html.{not_authorised, signedOut, unauthorised}

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends PlaySpec with ErsTestHelper with GuiceOneAppPerSuite {

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
  val unauthorisedView: unauthorised = app.injector.instanceOf[unauthorised]
  val signedOutView: signedOut = app.injector.instanceOf[signedOut]
  val notAuthorisedView: views.html.not_authorised = app.injector.instanceOf[not_authorised]

  override val testAuthActionGov: AuthActionGovGateway =
    new AuthActionGovGateway(mockAuthConnector, mockAppConfig, defaultParser)(ec) {
      override def invokeBlock[A](
                                   request: Request[A],
                                   block: RequestWithOptionalAuthContext[A] => Future[Result]
                                 ): Future[Result] =
        block(RequestWithOptionalAuthContext(request, defaultErsAuthData))
    }

  val testController = new ApplicationController(
    mockMCC,
    unauthorisedView,
    signedOutView,
    notAuthorisedView,
    mockSessionService,
    testAuthActionGov
  )

  "ApplicationController" must {

    "respond to /unauthorised" in {
      val result = route(app, FakeRequest(GET, "/submit-your-ers-annual-return/unauthorised"))
      status(result.get) mustBe UNAUTHORIZED
    }
  }

  "get /unauthorised" must {

    "have a status of Unauthorised" in {
      val result = testController.unauthorised().apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }

    "have a title of Unauthorised" in {
      val result = testController.unauthorised().apply(FakeRequest())
      contentAsString(result) must include("<title>Unauthorised</title>")
    }

    "have some text on the page" in {
      val result = testController.unauthorised().apply(FakeRequest())
      contentAsString(result) must include("You’re not authorised to view this page")
    }
  }

  //TODO content references ERS Checking for some reason - not changing just in case but needs investigating
  "get /not-authorised" must {

    "have a status of Unauthorised" in {
      val result = testController.notAuthorised().apply(FakeRequest())
      status(result) must be(UNAUTHORIZED)
    }

    "have a title of Check your ERS files" in {
      val result = testController.notAuthorised().apply(FakeRequest())
      contentAsString(result) must include("<title>Submit your ERS annual return</title>")
    }

    "have some text on the page" in {
      val result = testController.notAuthorised().apply(FakeRequest())
      contentAsString(result) must include("You’re not authorised to access ERS checking service")
    }
  }

  "get /signed-out" must {
    when(mockAppConfig.portalDomain).thenReturn("/")

    "have a status of Ok" in {
      val result = testController.timedOut().apply(FakeRequest())
      status(result) must be(OK)
    }

    "have a title of Submit your ERS annual return" in {
      val result = testController.timedOut().apply(FakeRequest())
      contentAsString(result) must include("<title>Submit your ERS annual return</title>")
    }

    "have some text on the page" in {
      val result = testController.timedOut().apply(FakeRequest())
      contentAsString(result) must include("For your security, we signed you out")
    }
  }

  "keepAlive" should {
    "return 200 OK when session data is found and cached" in {

      val testOptString: Option[String] = Some("test")

      val schemeInfo: SchemeInfo = SchemeInfo(
        testOptString.get,
        ZonedDateTime.now,
        testOptString.get,
        testOptString.get,
        testOptString.get,
        "CSOP"
      )
      val validErsMetaData: ErsMetaData =
        ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

      when(mockSessionService.fetch[ErsMetaData](any())(any(), any()))
        .thenReturn(Future.successful(validErsMetaData))

      when(mockSessionService.cache(any(), any())(any(), any()))
        .thenReturn(Future.successful(sessionPair))

      val result = testController.keepAlive.apply(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) must include("OK")
    }

    "return 500 Internal Server Error when no session data is found" in {
      when(mockSessionService.fetch[ErsMetaData](any())(any(), any()))
        .thenReturn(Future.successful(null))

      val result = testController.keepAlive.apply(FakeRequest())

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("Unexpected error")
    }

    "return 500 Internal Server Error on unexpected exception" in {
      when(mockSessionService.fetch[ErsMetaData](any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("DB failure")))

      val result = testController.keepAlive.apply(FakeRequest())

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("Unexpected error")
    }

    "return 500 Internal Server Error when session data is fetched but caching fails" in {
      val testOptString: Option[String] = Some("test")

      val schemeInfo: SchemeInfo = SchemeInfo(
        testOptString.get,
        ZonedDateTime.now,
        testOptString.get,
        testOptString.get,
        testOptString.get,
        "CSOP"
      )

      val validErsMetaData: ErsMetaData =
        ErsMetaData(schemeInfo, "ipRef", Some("aoRef"), "empRef", Some("agentRef"), Some("sapNumber"))

      when(mockSessionService.fetch[ErsMetaData](any())(any(), any()))
        .thenReturn(Future.successful(validErsMetaData))

      when(mockSessionService.cache(any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Cache write failure")))

      val result = testController.keepAlive.apply(FakeRequest())

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("Unexpected error")
    }
  }
}
