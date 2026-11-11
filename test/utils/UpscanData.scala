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

import models.upscan.*
import models.upscan.UpscanCallback.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import java.net.URL
import java.time.Instant

trait UpscanData {

  val testUploadId: UploadId                     = UploadId("TestUploadId")
  val notStartedCallback: UpscanCsvFilesCallback = UpscanCsvFilesCallback(testUploadId, "file4", NotStarted)

  val uploadedSuccessfully: UploadedSuccessfully =
    UploadedSuccessfully(
      "fileName.ods",
      "https://downloadUrl.com",
      mimeType = Some("application/vnd.oasis.opendocument.spreadsheet")
    )

  val uploadedSuccessfullyInvalid: UploadedSuccessfully =
    UploadedSuccessfully(
      "test.txt",
      "https://downloadUrl.com",
      mimeType = Some("text/plain")
    )

  val uploadedSuccessfullyCsv: UploadedSuccessfully =
    UploadedSuccessfully("fileName.csv", "https://downloadUrl.com", mimeType = Some("text/csv"))

  /**
    * Provides an upcast of UploadStatus ADT members.<p/>
    * The upcast is needed due to the Play Reads and Writes typeclasses becoming invariant in v. 2.8.<p/>
    * @param status An UploadStatus value
    * @return an UplaodStatus reference
    */
  def asUploadStatus(status: UploadStatus): UploadStatus = status

  val incompleteCsvList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(UploadId("ID1"), "file1", InProgress),
      notStartedCallback
    )
  )

  val notStartedCsvList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", NotStarted),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
    )
  )

  val incompleteCsvList2: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", InProgress),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
    )
  )

  val incompleteCsvList3: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", uploadedSuccessfully),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
    )
  )

  val failedCsvList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", Failed),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", Failed)
    )
  )

  val successfulCsvList: UpscanCsvFilesCallbackList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", uploadedSuccessfully)
    )
  )

  val uploadDetails: UploadDetails = UploadDetails(Instant.now(), "checksum", Some("fileMimeType"), "fileName", 100)

  val readyCallback: UpscanReadyCallback =
    UpscanReadyCallback(Reference("Reference"), new URL("https://callbackUrl.com"), uploadDetails)

  val failedCallback: UpscanFailedCallback =
    UpscanFailedCallback(Reference("Reference"), ErrorDetails("failureReason", "message"))

  import models.upscan.UpscanCallback.*

  given failedWrites: OWrites[UpscanFailedCallback] =
    OWrites[UpscanFailedCallback] { callback =>
      Json.obj(
        "reference"      -> JsString(callback.reference.value),
        "failureDetails" -> Json.obj(
          "failureReason" -> callback.failureDetails.failureReason,
          "message"       -> callback.failureDetails.message
        ),
        "fileStatus"     -> "FAILED"
      )
    }

  given readyWrites: OWrites[UpscanReadyCallback] =
    OWrites[UpscanReadyCallback] { callback =>
      Json.obj(
        "reference"     -> JsString(callback.reference.value),
        "downloadUrl"   -> JsString(callback.downloadUrl.toString),
        "uploadDetails" -> Json.toJson(callback.uploadDetails)(
          UpscanCallback.uploadDetailsFormat
        ),
        "fileStatus"    -> "READY"
      )
    }

  val upscanInitiateResponse: UpscanInitiateResponse =
    UpscanInitiateResponse(Reference("reference"), "postTarget", formFields = Map.empty[String, String])

  val inProgressUpscanCsvFilesList: UpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", InProgress)
    )
  )

  val notStartedUpscanCsvFilesList: UpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", NotStarted)
    )
  )

  val multipleNotStartedUpscanCsvFilesList: UpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", NotStarted),
      UpscanIds(UploadId("ID1"), "file2", NotStarted)
    )
  )

  val multipleInPrgoressUpscanCsvFilesList: UpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", InProgress),
      UpscanIds(UploadId("ID1"), "file2", InProgress)
    )
  )

}
