/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import utils.Fixtures

class GroupSummaryDecoratorSpec extends AnyWordSpecLike with Matchers with OptionValues with MockitoSugar  {

  implicit val messages: Messages = mock[Messages]

  "GroupSummary Decorator" should {

    "not add anything if companies is not defined" in {
      val decorator = new GroupSummaryDecorator("title", None)

      val output = decorator.decorate

      output shouldBe ""
    }

    "add title and company name to section" in {
      val decorator = new GroupSummaryDecorator("title", Fixtures.ersSummary.companies)

      val output = decorator.decorate

      output.contains("title") shouldBe true
      output.contains("testCompany") shouldBe true
      output.contains("<hr/>") shouldBe true
     }
   }
}
