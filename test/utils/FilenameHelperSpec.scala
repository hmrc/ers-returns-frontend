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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec

class FileNameHelperSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {

  "FileNameHelper.isIncorrectFileName" should {

    "accept names made only of letters, digits, underscores, dots and hyphens" in {
      val validNames = Table(
        "fileName",
        "EMI_Template.ods",
        "csop-v5.ods",
        "SAYE_2025-26.ods",
        "a.ods",
        "UPPER_and_lower_123.ODS",
        "no-extension"
      )

      forAll(validNames) { name =>
        FileNameHelper.isIncorrectFileName(name) shouldBe false
      }
    }

    "accept a name of exactly 240 characters" in {
      val name = "a" * 236 + ".ods"
      name.length                              shouldBe 240
      FileNameHelper.isIncorrectFileName(name) shouldBe false
    }

    "reject a name longer than 240 characters" in {
      val name = "a" * 237 + ".ods"
      name.length                              shouldBe 241
      FileNameHelper.isIncorrectFileName(name) shouldBe true
    }

    "reject an empty name" in {
      FileNameHelper.isIncorrectFileName("") shouldBe true
    }

    // mirrors deny-list on the ODS upload page's JS (checkFileName method)
    "reject names containing any character on the deny-list" in {
      val deniedCharacters = Seq(
        "folder/file.ods", // "/"
        "a^b.ods", // "^"
        "a~b.ods", // "~"
        "a\"b.ods", // "\""
        "a|b.ods", // "|"
        "file#1.ods", // "#"
        "what?.ods", // "?"
        "a,b.ods", // ","
        "a]b.ods", // "]"
        "a[b.ods", // "["
        "cost£.ods", // "£"
        "a$b.ods", // "$"
        "Smith & Co.ods", // "&"
        "a:b.ods", // ":"
        "a@b.ods", // "@"
        "a*b.ods", // "*"
        "C:\\Users\\me\\file.ods", // "\\"
        "a+b.ods", // "+"
        "50%.ods", // "%"
        "a{b}.ods", // "{ }"
        "<script>.ods" //  "< >"
      )

      deniedCharacters.foreach { name =>
        FileNameHelper.isIncorrectFileName(name) shouldBe true
      }
    }

    "accept names the browser JS allows" in {
      val allowedByJs = Table(
        "fileName",
        "ERS return 2025.ods",
        "EMI_Template (1).ods",
        "Smith's return.ods",
        "Société.ods",
        "Ŵyn.ods",
        "Return – final.ods",
        "Q1=final.ods",
        "v2!.ods",
        "a;b.ods",
        " leading-space.ods"
      )

      forAll(allowedByJs) { name =>
        FileNameHelper.isIncorrectFileName(name) shouldBe false
      }
    }
  }

}
