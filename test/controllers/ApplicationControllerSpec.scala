/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ErsTestHelper
import views.html.{not_authorised, signedOut, unauthorised}

import scala.concurrent.ExecutionContext

class ApplicationControllerSpec extends PlaySpec with ErsTestHelper with GuiceOneAppPerSuite{

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)

  implicit lazy val materializer: Materializer = app.materializer
  val unauthorisedView: unauthorised = app.injector.instanceOf[unauthorised]
  val signedOutView: signedOut = app.injector.instanceOf[signedOut]
  val notAuthorisedView: views.html.not_authorised = app.injector.instanceOf[not_authorised]

	val testController = new ApplicationController(mockMCC, mockAuthConnector, mockErsUtil, mockAppConfig, unauthorisedView, signedOutView, notAuthorisedView, testAuthActionGov)

  "ApplicationController" must {

    "respond to /unauthorised" in {
      val result = route(app, FakeRequest(GET, "/submit-your-ers-annual-return/unauthorised"))
      status(result.get) must not equal NOT_FOUND
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
}
