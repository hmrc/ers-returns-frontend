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

package services

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import services.pdf.ErsReceiptPdfBuilderService
import utils.{ContentUtil, ERSFakeApplicationConfig, ERSUtil, ErsTestHelper, Fixtures}

import scala.concurrent.ExecutionContext

class ErsReceiptPdfBuilderServiceSpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with MockitoSugar
  with BeforeAndAfterEach
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
  implicit val ersUtil: ERSUtil = mockErsUtil
	val testErsReceiptPdfBuilderService = new ErsReceiptPdfBuilderService(mockCountryCodes)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mockMCC.messagesApi)

  "ErsReceiptPdfBuilderService" should {
    "generate the ERS summary metdata" in {
      when(mockErsUtil.replaceAmpersand(any[String])).thenAnswer(_.getArgument(0))
			val output = testErsReceiptPdfBuilderService.addMetaData(Fixtures.ersSummary, "12 August 2016, 4:28pm")

      val expectedConfirmationMessage = s"Your ${ContentUtil.getSchemeAbbreviation("emi")} annual return has been submitted."

      output.contains(expectedConfirmationMessage) shouldBe true
      output.contains("Scheme name") shouldBe true
      output.contains("My scheme") shouldBe true
      output.contains("testbundle") shouldBe true
      output.contains("Time and date of submission") shouldBe true
      output.contains("4:28PM on Fri 12 August 2016") shouldBe true
    }
  }
}
