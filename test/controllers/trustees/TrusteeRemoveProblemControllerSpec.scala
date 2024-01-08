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

package controllers.trustees

import controllers.auth.RequestWithOptionalAuthContext
import models.RequestObject
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status, stubBodyParser}
import utils.Fixtures.ersRequestObject
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}
import views.html.{global_error, trustee_remove_problem}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeRemoveProblemControllerSpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite
  with ScalaFutures {

  implicit val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)

  val testController = new TrusteeRemoveProblemController(
    mockMCC,
    testAuthAction,
    mockErsUtil,
    app.injector.instanceOf[trustee_remove_problem],
    app.injector.instanceOf[global_error]
  )(mockMCC.executionContext, mockAppConfig)

  "onPageLoad" should {
    val authRequest: RequestWithOptionalAuthContext[AnyContent] = buildRequestWithAuth(Fixtures.buildFakeRequestWithSessionIdSIP("GET"))
    setAuthMocks()

    "show trusteeRemoveProblemView" in {
      when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.successful(ersRequestObject))

      val result = testController.onPageLoad().apply(authRequest)

      status(result) shouldBe Status.OK

      contentAsString(result) should include(testMessages("ers_trustee_remove_problem.title"))
      contentAsString(result) should include(testMessages("ers_trustee_remove_problem.p1"))
      contentAsString(result) should include(testMessages("ers.continue"))
    }

    "show getGlobalErrorPage if fetch failed" in {
      when(mockErsUtil.fetch[RequestObject](any())(any(), any(), any())).thenReturn(Future.failed(new Exception("exception")))

      val result = testController.onPageLoad().apply(authRequest)

      status(result) shouldBe Status.OK

      contentAsString(result) should include(testMessages("ers.global_errors.title"))
      contentAsString(result) should include(testMessages("ers.global_errors.heading"))
      contentAsString(result) should include(testMessages("ers.global_errors.message"))
    }
  }

}
