/*
 * Copyright 2022 HM Revenue & Customs
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

package services.pdf

import akka.stream.Materializer
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.{ERSFakeApplicationConfig, ErsTestHelper}

import scala.concurrent.ExecutionContext

class YesNoDecoratorSpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with MockitoSugar
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val mat: Materializer = app.materializer
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)


  "nil returns decorator" should {
    "show Yes if there is nil return" in {
      val decorator = new YesNoDecorator("title", "1")

      val output = decorator.decorate

      output.contains("title") shouldBe true
      output.contains("Yes") shouldBe true
      output.contains("<hr/>") shouldBe true
    }

    "show No if there is no nil return" in {
      val decorator = new YesNoDecorator("title", "2")

      val output = decorator.decorate

      output.contains("title") shouldBe true
      output.contains("No") shouldBe true
      output.contains("<hr/>") shouldBe true
    }
   }

}
