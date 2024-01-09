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

package services.pdf

import akka.stream.Materializer
import models._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser
import utils.{ERSFakeApplicationConfig, ErsTestHelper}

import scala.concurrent.ExecutionContext

class AlterationsAmendsDecoratorSpec
    extends AnyWordSpecLike
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
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val mat: Materializer = app.materializer

  lazy val altAmends: AlterationAmends = AlterationAmends(
    altAmendsTerms = Some("1"),
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

    "stream nothing if map is empty" in {
      val decorator = new AlterationsAmendsDecorator(Map[String, String]())

      val output = decorator.decorate

      output shouldBe ""

    }

    "stream csop alterations amends title and given fields" in {

      val decorator = new AlterationsAmendsDecorator(map)

      val output = decorator.decorate

      output.contains(Messages("ers_trustee_summary.altamends.section")) shouldBe true
      output.contains(Messages("ers_alt_amends.csop.option_1"))          shouldBe true
      output.contains(Messages("ers_alt_amends.csop.option_2"))          shouldBe false
      output.contains(Messages("ers_alt_amends.csop.option_3"))          shouldBe true
      output.contains(Messages("ers_alt_amends.csop.option_4"))          shouldBe false
      output.contains(Messages("ers_alt_amends.csop.option_5"))          shouldBe true
    }
  }
}
