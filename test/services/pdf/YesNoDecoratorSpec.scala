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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
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
      val streamer = mock[ErsContentsStreamer]
      val decorator = new YesNoDecorator("title", "1", 1.0f, 2.0F, 3.0F, 4.0F)

      decorator.decorate(streamer)(testMessages)

      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("title": String), ArgumentMatchers.eq(1.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("Yes": String), ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
    }

    "show No if there is no nil return" in {
      val streamer = mock[ErsContentsStreamer]
      val decorator = new YesNoDecorator("title", "2", 1.0f, 2.0F, 3.0F, 4.0F)

      decorator.decorate(streamer)(testMessages)

      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("title": String), ArgumentMatchers.eq(1.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("No": String), ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
    }

    "show section divider after the block is rendered" in {
      val streamer = mock[ErsContentsStreamer]
      val decorator = new YesNoDecorator("title", "2", 1.0f, 2.0F, 3.0F, 4.0F)

      decorator.decorate(streamer)(testMessages)

      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(3.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(2)).drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(4.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawLine()
    }
   }

}
