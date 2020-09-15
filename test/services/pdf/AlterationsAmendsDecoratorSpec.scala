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
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec
import utils.ERSFakeApplicationConfig

class AlterationsAmendsDecoratorSpec extends UnitSpec with MockitoSugar with ERSFakeApplicationConfig with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer

  lazy val altAmends = AlterationAmends(altAmendsTerms = Some("1"),
    altAmendsEligibility = None,
    altAmendsExchange = Some("1"),
    altAmendsVariations = None,
    altAmendsOther = Some("1")
  )

  lazy val map: Map[String, String] = Map(
    ("title", Messages("ers_trustee_summary.altamends.section")),
    ("option1", Messages("ers_alt_amends.csop.option_1")),
    ("option3", Messages("ers_alt_amends.csop.option_3")),
    ("option5", Messages("ers_alt_amends.csop.option_5"))
  )


  "alterations amends decorator" should {

    "draw a block divider" in {
      val decorator = new AlterationsAmendsDecorator(map, 1.0f, 2.0F, 3.0F, 4.0F)
      val streamer = mock[ErsContentsStreamer]

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(2)).drawText(org.mockito.ArgumentMatchers.eq(Messages(""): String), org.mockito.ArgumentMatchers.eq(4.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawLine()
    }

    "stream nothing if map is empty" in {
      val decorator = new AlterationsAmendsDecorator(Map[String, String](), 1.0f, 2.0F, 3.0F, 4.0F)
      val streamer = mock[ErsContentsStreamer]

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(0)).drawText(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyFloat())(ArgumentMatchers.any())
    }

    "stream csop alterations amends title and given fields" in {

      val decorator = new AlterationsAmendsDecorator(map, 1.0f, 2.0F, 3.0F, 4.0F)
      val streamer = mock[ErsContentsStreamer]

      decorator.decorate(streamer)

      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(Messages("ers_trustee_summary.altamends.section"): String), org.mockito.ArgumentMatchers.eq(1.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(s"${Messages("ers_alt_amends.csop.option_1")}.": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(0)).drawText(org.mockito.ArgumentMatchers.eq(s"${Messages("ers_alt_amends.csop.option_2")}.": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(s"${Messages("ers_alt_amends.csop.option_3")}.": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(0)).drawText(org.mockito.ArgumentMatchers.eq(s"${Messages("ers_alt_amends.csop.option_4")}.": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())
      verify(streamer, VerificationModeFactory.times(1)).drawText(org.mockito.ArgumentMatchers.eq(s"${Messages("ers_alt_amends.csop.option_5")}.": String), org.mockito.ArgumentMatchers.eq(2.0F: Float))(ArgumentMatchers.any())

    }
  }
}
