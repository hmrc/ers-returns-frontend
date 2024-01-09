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

import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{OptionValues, PrivateMethodTester}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ErsTestHelper

import scala.concurrent.ExecutionContext

class LanguageSwitchControllerSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with PrivateMethodTester {

  override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  lazy val langMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  when(mockAppConfig.languageTranslationEnabled).thenReturn(true)
  when(mockAppConfig.languageMap).thenReturn(langMap)

  "Hitting language selection endpoint" must {
    "redirect to Welsh translated start page if Welsh language is selected" in {
      val request                = FakeRequest()
      val result                 = new LanguageSwitchController(appConfig = mockAppConfig, mockMCC).switchToLanguage("cymraeg")(request)
      val resultCookies: Cookies = cookies(result)
      resultCookies.size shouldBe 1
      val cookie: Cookie = resultCookies.head
      cookie.name  shouldBe "PLAY_LANG"
      cookie.value shouldBe "cy"
    }

    "redirect to English translated start page if English language is selected" in {
      val request                = FakeRequest()
      val result                 = new LanguageSwitchController(appConfig = mockAppConfig, mockMCC).switchToLanguage("english")(request)
      val resultCookies: Cookies = cookies(result)
      resultCookies.size shouldBe 1
      val cookie: Cookie = resultCookies.head
      cookie.name  shouldBe "PLAY_LANG"
      cookie.value shouldBe "en"
    }
  }

  "asRelativeUrl method" should {

    "build the relative url given valid input" in {
      val controller    = new LanguageSwitchController(mockAppConfig, mockMCC)
      val privateMethod = PrivateMethod[Option[String]](Symbol("asRelativeUrl"))
      val result        = controller.invokePrivate(privateMethod("http://localhost:9000/test?testing#testerino"))

      result.get shouldBe "/test?testing#testerino"
    }

  }
}
