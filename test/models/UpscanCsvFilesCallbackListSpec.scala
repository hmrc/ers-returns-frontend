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

package models

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utils.UpscanData

class UpscanCsvFilesCallbackListSpec extends AnyWordSpecLike with Matchers with OptionValues with UpscanData {

  "areAllFilesComplete" should {
    "return true for failed or successful uploads" in {
      failedCsvList.areAllFilesComplete()     shouldBe true
      successfulCsvList.areAllFilesComplete() shouldBe true
    }

    "return false" when {
      "all files have status of InProgress or NotStarted" in {
        incompleteCsvList.areAllFilesComplete() shouldBe false
      }

      "one file is not started or inprogress" in {
        incompleteCsvList3.areAllFilesComplete() shouldBe false
      }
    }
  }

  "areAllFilesSuccessful" should {
    "return true" when {
      "all files are uploaded successfully" in {
        successfulCsvList.areAllFilesSuccessful() shouldBe true
      }
    }

    "return false" when {
      "uploads are not complete" in {
        incompleteCsvList.areAllFilesSuccessful() shouldBe false
      }

      "upload has failed" in {
        failedCsvList.areAllFilesSuccessful() shouldBe false
      }
    }
  }

}
