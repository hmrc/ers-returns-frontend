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

package services.pdf

import models.{TrusteeDetails, TrusteeDetailsList}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages

import utils.{ERSFakeApplicationConfig, ErsTestHelper}

class TrusteeDecoratorSpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with MockitoSugar
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite {

  "Trusstees Decorator" should {

    "add title and trustee's name to section" in {
      val trusteeList = new TrusteeDetailsList(List(new TrusteeDetails("trustee name", "address", None, None, None, None, None)))
      val decorator = new TrusteesDecorator(Some(trusteeList))
      val output = decorator.decorate

      output.contains(Messages("ers_trustee_summary.title")) shouldBe true
      output.contains("trustee name") shouldBe true
      output.contains("<hr/>") shouldBe true
    }

    "not add trustee names if list is empty" in {
      val decorator = new TrusteesDecorator(None)

      val output = decorator.decorate

      output shouldBe ""
    }
  }
}
