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

package services.pdf

import akka.stream.Materializer
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.{ERSFakeApplicationConfig, ErsTestHelper, Fixtures}

import scala.concurrent.ExecutionContext

class ApachePdfContentsStreamerSpec extends WordSpecLike with Matchers with OptionValues with MockitoSugar with ERSFakeApplicationConfig with ErsTestHelper with GuiceOneAppPerSuite {

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
  implicit lazy val messages: Messages = testMessages.messages


  implicit lazy val mat: Materializer = app.materializer

  val ersSummary = Fixtures.ersSummary
  val streamer = new ApachePdfContentsStreamer(ersSummary)

  "Apache Pdf Contents Streamer" should {


    "return false if contents can not be saved" in {
      streamer.savePageContent() shouldEqual false
    }

     "return saved given ErsSummary as ByteStream" in {
       val output = streamer.saveErsSummary().size() should be > 0
    }

    "return true if page is created successfully" in {
      val streamer = new ApachePdfContentsStreamer(ersSummary)

      streamer.createNewPage() shouldEqual true
    }

     "return true if text is drawn successfully" in {
      val streamer = new ApachePdfContentsStreamer(ersSummary)
       streamer.createNewPage()

      streamer.drawText("blah", 1.0F) shouldEqual true
    }

    "return true if text that span more that one line is drawn successfully" in {
      val streamer = new ApachePdfContentsStreamer(ersSummary)
       streamer.createNewPage()
      streamer.drawText("text text text text text text text text text text text text ", 40.0F) shouldEqual true
    }

    "return true if line is drawn successfully" in {
      val streamer = new ApachePdfContentsStreamer(ersSummary)
       streamer.createNewPage()
      streamer.drawLine() shouldEqual true
    }


    "return true if contents is saved successfully" in {
      val streamer = new ApachePdfContentsStreamer(ersSummary)
       streamer.createNewPage()
      streamer.savePageContent() shouldEqual true
    }

 }
}
