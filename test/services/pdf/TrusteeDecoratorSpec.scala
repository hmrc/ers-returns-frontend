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
import models.{TrusteeDetails, TrusteeDetailsList}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.{ERSFakeApplicationConfig, ErsTestHelper}

import scala.concurrent.ExecutionContext

class TrusteeDecoratorSpec extends WordSpecLike with Matchers with OptionValues with MockitoSugar with ERSFakeApplicationConfig with ErsTestHelper with GuiceOneAppPerSuite {

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

  lazy val trusteeList = new TrusteeDetailsList(List(new TrusteeDetails("name", "address", None, None, None, None, None)))
  lazy val decorator = new TrusteesDecorator(Some(trusteeList), 1.0F, 2.0F, 3.0F, 4.0F)

  "Trusstees Decorator" should {

    "draw block spacer at end of section" in {
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(2)).drawText(org.mockito.ArgumentMatchers.eq("": String), org.mockito.ArgumentMatchers.eq(4.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawLine()

    }

    "add title to section" in {
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(Messages("ers_trustee_summary.title"): String), org.mockito.ArgumentMatchers.eq(1.0F: Float))(ArgumentMatchers.any())
    }

    "add trustee name to the section" in {
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq("name": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
    }

    "not add trustee names if list is empty" in {
      val decorator = new TrusteesDecorator(None, 1.0F, 2.0F, 3.0F, 4.0F)
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(0)).drawText(org.mockito.ArgumentMatchers.eq(Messages("ers_trustee_summary.title"): String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
    }
  }
}
