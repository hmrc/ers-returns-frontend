/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.{ArgumentMatchers, Mock}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.Injector
import utils.Fixtures


class GroupSummaryDecoratorSpec extends UnitSpec with MockitoSugar  {

  implicit val messages: Messages = mock[Messages]

  val decorator = new GroupSummaryDecorator("title", Fixtures.ersSummary.companies, 1.0F, 2.0F, 3.0F, 4.0F)

  "GroupSummary Decorator" should {

    "not add anything if companies is not defined" in {
      val decorator = new GroupSummaryDecorator("title", None, 1.0F, 2.0F, 3.0F, 4.0F)
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(0)).drawText(
        ArgumentMatchers.eq("title": String), ArgumentMatchers.eq(1.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(0)).drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(3.0F: Float))(ArgumentMatchers.any())

    }
    "add title to section" in {
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("title": String), ArgumentMatchers.eq(1.0F: Float))(ArgumentMatchers.any())
     }

    "add company name to section" in {
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(1)).drawText(ArgumentMatchers.eq("testCompany": String), ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
     }

    "add block spacer at the end of the section" in {
      val streamer = mock[ErsContentsStreamer]
      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(3)).drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(3.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(2)).drawText(ArgumentMatchers.eq("": String), ArgumentMatchers.eq(4.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawLine()

    }
   }
}
