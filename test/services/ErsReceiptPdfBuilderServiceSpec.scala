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
import utils._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor3

import scala.concurrent.ExecutionContext

class ErsReceiptPdfBuilderServiceSpec
    extends AnyWordSpecLike
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
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val mat: Materializer = app.materializer
  implicit val ersUtil: ERSUtil = mockErsUtil
  val testErsReceiptPdfBuilderService = new ErsReceiptPdfBuilderService(mockCountryCodes)

  val testCases: TableFor3[String, String, String] = Table(
    ("language", "inputDate", "expectedOutputDate"),
    ("en", "12 August 2016, 4:28pm", "4:28PM on Fri 12 August 2016"),
    ("cy", "12 August 2016, 4:28pm", "4:28yh ar ddydd Gwen 12 Awst 2016"),
    ("en", "15 January 2022, 9:20am", "9:20AM on Sat 15 January 2022"),
    ("cy", "15 January 2022, 9:20am", "9:20yb ar ddydd Sad 15 Ionawr 2022")
  )

  "ErsReceiptPdfBuilderService" should {
    forAll(testCases) { (language: String, inputDate: String, expectedOutputDate: String) =>
      s"generate the ERS summary metdata correctly when parsed the date: $inputDate and language: $language" in {
        val testMessages: MessagesImpl = MessagesImpl(i18n.Lang(language), mockMCC.messagesApi)
        when(mockErsUtil.replaceAmpersand(any[String])).thenAnswer(_.getArgument(0))
        val output = testErsReceiptPdfBuilderService.addMetaData(Fixtures.ersSummary, inputDate)(testMessages)

        val expectedConfirmationMessage =
          testMessages.messages(
            "ers.pdf.confirmation.submitted",
            testMessages.messages(ContentUtil.getSchemeAbbreviation("emi"))
          )
        val expectedSchemeName = testMessages.messages("ers.pdf.scheme")
        val expectedDateAndTime = testMessages.messages("ers.pdf.date_and_time")

        // static pdf fields which depend on language
        output.contains(expectedConfirmationMessage) shouldBe true
        output.contains(expectedSchemeName) shouldBe true
        output.contains(expectedDateAndTime) shouldBe true

        // pdf fields which depend on input
        output.contains("My scheme") shouldBe true
        output.contains("testbundle") shouldBe true
        output.contains(expectedOutputDate) shouldBe true
      }
    }
  }
}
