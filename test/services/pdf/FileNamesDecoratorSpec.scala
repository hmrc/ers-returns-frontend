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
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.{ERSFakeApplicationConfig, ErsTestHelper}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class FileNamesDecoratorSpec extends AnyWordSpecLike
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

  "file name decorator" should {
    "return an empty string when list of files is empty" in {
      val decorator = new FileNamesDecorator("2", Some(ListBuffer[String]()))

      val output = decorator.decorate

      output shouldBe ""
    }

    "return ods files names when nil return is false" in {
      val decorator = new FileNamesDecorator("1", Some(ListBuffer[String]("odsFile")))

      val output = decorator.decorate

      output.contains(Messages("ers_summary_declaration.file_name")) shouldBe true
      output.contains("odsFile") shouldBe true
      output.contains("<hr/>") shouldBe true
    }

    "show csv files names when nil return is true" in {
      val decorator = new FileNamesDecorator("1", Some(ListBuffer[String]("csvFile0", "csvFile1")))

      val output = decorator.decorate

      output.contains(Messages("ers_summary_declaration.file_name")) shouldBe true
      output.contains("csvFile0") shouldBe true
      output.contains("csvFile1") shouldBe true
      output.contains("<hr/>") shouldBe true

    }
  }
}
