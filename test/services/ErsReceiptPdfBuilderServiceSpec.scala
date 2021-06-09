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

package services

import akka.stream.Materializer
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import services.pdf.{DecoratorController, ErsContentsStreamer, ErsReceiptPdfBuilderService}
import utils.{ContentUtil, ERSFakeApplicationConfig, ErsTestHelper, Fixtures}

import java.io.ByteArrayOutputStream
import scala.concurrent.ExecutionContext

class ErsReceiptPdfBuilderServiceSpec extends WordSpecLike with Matchers with OptionValues with MockitoSugar with BeforeAndAfterEach
  with ERSFakeApplicationConfig with ErsTestHelper with GuiceOneAppPerSuite {

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
	val testErsReceiptPdfBuilderService = new ErsReceiptPdfBuilderService(mockCountryCodes)

  def verifyBlankBlock(streamer: ErsContentsStreamer) {
    verify(streamer, VerificationModeFactory.times(4))
			.drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(36.0F: Float))(ArgumentMatchers.any())
  }

  def verifyBlankLine(streamer: ErsContentsStreamer) {
    verify(streamer, VerificationModeFactory.times(4))
			.drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(12.0F: Float))(ArgumentMatchers.any())
  }

  "ErsReceiptPdfBuilderService" should {
    "ask the streamer to add ers summary metadata to the output pdf" in {

      implicit val streamer: ErsContentsStreamer = mock[ErsContentsStreamer]
      implicit val decorator: DecoratorController = mock[DecoratorController]

      when(streamer.drawText(anyString(), anyInt())(any())).thenReturn(true)
      when(streamer.saveErsSummary()).thenReturn(new ByteArrayOutputStream)
      when(streamer.savePageContent()).thenReturn(true)
      when(streamer.createNewPage()(any())).thenReturn(true)

			testErsReceiptPdfBuilderService.addMetaData(Fixtures.ersSummary, "12 August 2016, 4:28pm")

      val expectedConfirmationMessage = s"Your ${ContentUtil.getSchemeAbbreviation("emi")} " +
        s"annual return has been submitted for tax year 2014 to 2015."
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq(expectedConfirmationMessage: String), ArgumentMatchers.eq(16.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq("Scheme name:": String), ArgumentMatchers.eq(16.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq("My scheme": String), ArgumentMatchers.eq(12.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq("Reference code:": String), ArgumentMatchers.eq(16.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq("testbundle": String), ArgumentMatchers.eq(12.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq("Date and time submitted:": String), ArgumentMatchers.eq(16.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1))
				.drawText(ArgumentMatchers.eq("12 August 2016, 4:28PM": String), ArgumentMatchers.any())(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(4))
				.drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(36.0F: Float))(ArgumentMatchers.any())

      verifyBlankBlock(streamer)
      verifyBlankLine(streamer)
    }

    "ask the streamer to save the metadata to the output pdf" in {
      implicit val streamer: ErsContentsStreamer = mock[ErsContentsStreamer]
      implicit val decorator: DecoratorController = mock[DecoratorController]

      when(streamer.saveErsSummary()).thenReturn(new ByteArrayOutputStream)
      when(streamer.savePageContent()).thenReturn(true)
      when(streamer.createNewPage()).thenReturn(true)

			testErsReceiptPdfBuilderService.addMetaData(Fixtures.ersSummary, "12 August 2016, 12:28pm")
      verify(streamer, VerificationModeFactory.times(1)).savePageContent()
    }
  }
}
