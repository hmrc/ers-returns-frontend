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
import org.scalatest.wordspec.AnyWordSpec

class MimeTypeValidatorSpec extends AnyWordSpec with Matchers {

  "checkIsCSVMimeType" should {
    "return true for the CSV mime type" in {
      MimeTypeValidator.checkIsCSVMimeType(Some("text/csv")) shouldBe true
    }

    "return true regardless of case and surrounding whitespace" in {
      MimeTypeValidator.checkIsCSVMimeType(Some("  TEXT/CSV  ")) shouldBe true
    }

    "return false for a non-CSV mime type" in {
      MimeTypeValidator.checkIsCSVMimeType(Some("application/vnd.oasis.opendocument.spreadsheet")) shouldBe false
    }

    "return false for an empty mime type" in {
      MimeTypeValidator.checkIsCSVMimeType(Some("")) shouldBe false
    }

    "return true when the mime type is absent (falls back to filename validation)" in {
      MimeTypeValidator.checkIsCSVMimeType(None) shouldBe true
    }
  }

  "checkIsODSMimeType" should {
    "return true for the ODS mime type" in {
      MimeTypeValidator.checkIsODSMimeType(Some("application/vnd.oasis.opendocument.spreadsheet")) shouldBe true
    }

    "return true regardless of case and surrounding whitespace" in {
      MimeTypeValidator.checkIsODSMimeType(Some("  APPLICATION/VND.OASIS.OPENDOCUMENT.SPREADSHEET  ")) shouldBe true
    }

    "return false for a non-ODS mime type" in {
      MimeTypeValidator.checkIsODSMimeType(Some("text/csv")) shouldBe false
    }

    "return false for an empty mime type" in {
      MimeTypeValidator.checkIsODSMimeType(Some("")) shouldBe false
    }

    "return true when the mime type is absent (falls back to filename validation)" in {
      MimeTypeValidator.checkIsODSMimeType(None) shouldBe true
    }
  }

}
