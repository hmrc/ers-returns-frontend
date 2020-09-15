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

import akka.stream.Materializer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec
import utils.ERSFakeApplicationConfig

import scala.collection.mutable.ListBuffer

class FileNamesDecoratorSpec extends UnitSpec with MockitoSugar with ERSFakeApplicationConfig with OneAppPerSuite {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer

  "file name decorator" should {
    "not show file names when nil reuturn is false" in {
      val decorator = new FileNamesDecorator("2", Some(ListBuffer[String]()), 0.0F, 0.0F, 0.0F, 0.0F)
      val streamer = mock[ErsContentsStreamer]

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(0)).drawText(any(), any())(any())
    }

    "show ods files names when nil return is true" in {
      val streamer = mock[ErsContentsStreamer]
      val decorator = new FileNamesDecorator("1", Some(ListBuffer[String]("odsFile")), 1.0F, 2.0F, 3.0F, 4.0F)

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(Messages("ers_summary_declaration.file_name"): String), org.mockito.ArgumentMatchers.eq(1.0F: Float))(any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq("odsFile": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(any())
    }

    "show csv files names when nil return is true" in {
      val streamer = mock[ErsContentsStreamer]
      val decorator = new FileNamesDecorator("1", Some(ListBuffer[String]("csvFile0", "csvFile1")), 1.0F, 2.0F, 3.0F, 4.0F)

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(Messages("ers_summary_declaration.file_names"): String), org.mockito.ArgumentMatchers.eq(1.0F: Float))(any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq("csvFile0": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq("csvFile1": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(any())
    }

    "show block spacer at the end" in {
      val streamer = mock[ErsContentsStreamer]
      val decorator = new FileNamesDecorator("1", Some(ListBuffer[String]("odsFile")), 1.0F, 2.0F, 3.0F, 4.0F)

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(2)).drawText(org.mockito.ArgumentMatchers.eq("": String), org.mockito.ArgumentMatchers.eq(4.0F: Float))(any())
      verify(streamer, VerificationModeFactory.times(1)).drawLine()
    }
  }
}
