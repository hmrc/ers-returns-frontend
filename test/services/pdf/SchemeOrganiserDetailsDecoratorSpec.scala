/*
 * Copyright 2026 HM Revenue & Customs
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

import models.SchemeOrganiserDetails
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import utils.CountryCodes

class SchemeOrganiserDetailsDecoratorSpec
    extends AnyWordSpecLike with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite {

  implicit val messages: Messages    = mock[Messages]
  val mockCountryCodes: CountryCodes = app.injector.instanceOf[CountryCodes]

  "Company details title decorator" should {

    "add company details to the ers stream" in {
      val schemeOrganiser: SchemeOrganiserDetails = SchemeOrganiserDetails(
        "companyName",
        "addressLine1",
        Some("addressLine2"),
        Some("addressLine3"),
        Some("addressLine4"),
        None,
        Some("post code"),
        Some("company reg"),
        Some("corporationRef")
      )

      val decorator = new SchemeOrganiserDetailsDecorator("title", schemeOrganiser, mockCountryCodes)

      val output = decorator.decorate

      output.contains("title")          shouldBe true
      output.contains("companyName")    shouldBe true
      output.contains("addressLine1")   shouldBe true
      output.contains("addressLine2")   shouldBe true
      output.contains("addressLine3")   shouldBe true
      output.contains("addressLine4")   shouldBe true
      output.contains("post code")      shouldBe true
      output.contains("company reg")    shouldBe true
      output.contains("corporationRef") shouldBe true
      output.contains("<hr/>")          shouldBe true
    }
  }

}
