/*
 * Copyright 2021 HM Revenue & Customs
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

import models.upscan.UploadId
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import utils.UpscanData

class UpscanCsvFilesListSpec extends WordSpecLike with Matchers with OptionValues with UpscanData {

  "updateToInProgress" should {
    "update a NotStarted upload with matching ID to InProgress" in {
      notStartedUpscanCsvFilesList.updateToInProgress(testUploadId) shouldBe inProgressUpscanCsvFilesList
    }

    "throw an exception" when {
      "there is no corresponding upload ID" in {
        an [Exception] should be thrownBy {
          inProgressUpscanCsvFilesList.updateToInProgress(testUploadId)
        }
      }

      "the upload ID does not have a status of NotStarted" in {
        an [Exception] should be thrownBy {
          notStartedUpscanCsvFilesList.updateToInProgress(UploadId("TEST-ID"))
        }
      }
    }
  }

  "noOfUploads" should {
    "count the number of InProgress uploads" in {
      multipleNotStartedUpscanCsvFilesList.noOfUploads shouldBe 0
      inProgressUpscanCsvFilesList.noOfUploads shouldBe 1
    }
  }

  "noOfFilesToUpload" should {
    "count the number of files to upload" in {
      multipleNotStartedUpscanCsvFilesList.noOfFilesToUpload shouldBe 2
      inProgressUpscanCsvFilesList.noOfFilesToUpload shouldBe 1
    }
  }

}
