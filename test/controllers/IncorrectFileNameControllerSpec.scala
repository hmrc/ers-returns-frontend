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

import config.ApplicationConfig
import models._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ErsTestHelper
import utils.Fixtures.ersRequestObject
import views.html.incorrect_file_name

import scala.concurrent.{ExecutionContext, Future}

class IncorrectFileNameControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with ScalaFutures {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]())(ExecutionContext.global),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  val incorrectFileNameView: incorrect_file_name = app.injector.instanceOf[incorrect_file_name]
  implicit lazy val testMessages: MessagesImpl   = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)
  implicit lazy val mat: Materializer            = app.materializer
  implicit val appConfig: ApplicationConfig      = app.injector.instanceOf[ApplicationConfig]
  implicit val actorSystem: ActorSystem          = app.actorSystem

  override def beforeEach(): Unit =
    reset(mockErsUtil)

  setAuthMocks()

  "calling fileNameValidationFailure" should {

    "return BAD_REQUEST and render incorrect file name page" in {

      when(
        mockSessionService.fetch[RequestObject](any())(any(), any())
      ) thenReturn Future.successful(ersRequestObject)

      val controller =
        new IncorrectFileNameController(mockMCC, mockSessionService, incorrectFileNameView, testAuthAction)(
          ec,
          mockErsUtil,
          appConfig
        )

      val result = controller.incorrectFileNamePage().apply(FakeRequest("GET", ""))
      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include(
        testMessages("ers.incorrect_file_name.title")
      )
    }
  }

}
