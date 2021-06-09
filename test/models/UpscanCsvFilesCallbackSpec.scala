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

import models.upscan._
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import utils.UpscanData

class UpscanCsvFilesCallbackSpec extends WordSpecLike with Matchers with OptionValues with UpscanData {

  "isStarted" should {
    "return true" when {
      val otherStatuses = List(InProgress, Failed, uploadedSuccessfully)
      otherStatuses.foreach { status =>
        s"uploadStatus is ${status.getClass.getSimpleName}" in {
          callbackWithStatus(status).isStarted shouldBe true
        }
      }
    }

    "return false" when {
      "upload status is NotStarted" in {
        callbackWithStatus(NotStarted).isStarted shouldBe false
      }
    }
  }

  "isComplete" should {
    "return true" when {
      val trueStatuses = List(Failed, uploadedSuccessfully)
      trueStatuses.foreach { status =>
        s"uploadStatus is ${status.getClass.getSimpleName}" in {
          callbackWithStatus(status).isComplete shouldBe true
        }
      }
    }

    "return false" when {
      val falseStatuses = List(NotStarted, InProgress)
      falseStatuses.foreach { status =>
        s"uploadStatus is ${status.getClass.getSimpleName}" in {
          callbackWithStatus(status).isComplete shouldBe false
        }
      }
    }
  }

  def callbackWithStatus(uploadStatus: UploadStatus) =
    UpscanCsvFilesCallback(testUploadId, "FileId", uploadStatus)
}
