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
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ERSFakeApplicationConfig

class ApplicationControllerSpec extends PlaySpec with OneServerPerSuite with ERSFakeApplicationConfig with ErsTestHelper {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
	val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val materializer: Materializer = app.materializer

	val testController = new ApplicationController(messagesApi, mockAuthConnector, mockErsUtil, mockAppConfig)

  "ApplicationController" must {

    "respond to /unauthorised" in {
      val result = route(FakeRequest(GET, "/submit-your-ers-annual-return/unauthorised"))
      status(result.get) must not equal NOT_FOUND
    }
  }

  "get /unauthorised" must {

    "have a status of OK" in {
      val result = testController.unauthorised().apply(FakeRequest())
      status(result) must be(OK)
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
