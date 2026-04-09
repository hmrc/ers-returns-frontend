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

import models.upscan._
import play.api.libs.json._

import java.net.URL
import java.time.Instant

trait UpscanData {

  val testUploadId: UploadId                        = UploadId("TestUploadId")
  val notStartedCallback: UpscanCsvFilesCallback    = UpscanCsvFilesCallback(testUploadId, "file4", NotStarted)
  val uploadedSuccessfully: UploadedSuccessfully    = UploadedSuccessfully("fileName.ods", "https://downloadUrl.com")
  val uploadedSuccessfullyCsv: UploadedSuccessfully = UploadedSuccessfully("fileName.csv", "https://downloadUrl.com")

  /**
    * Provides an upcast of UploadStatus ADT members.<p/>
    * The upcast is needed due to the Play Reads and Writes typeclasses becoming invariant in v. 2.8.<p/>
    * @param status An UploadStatus value
    * @return an UplaodStatus reference
    */
  def asUploadStatus(status: UploadStatus): UploadStatus = status

  val incompleteCsvList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(UploadId("ID1"), "file1", InProgress),
      notStartedCallback
    )
  )

  val notStartedCsvList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", NotStarted),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
    )
  )

  val incompleteCsvList2 = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", InProgress),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
    )
  )

  val incompleteCsvList3 = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", uploadedSuccessfully),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", NotStarted)
    )
  )

  val failedCsvList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", Failed),
      UpscanCsvFilesCallback(UploadId("ID2"), "file4", Failed)
    )
  )

  val successfulCsvList = UpscanCsvFilesCallbackList(
    List(
      UpscanCsvFilesCallback(testUploadId, "file1", uploadedSuccessfully)
    )
  )

  val uploadDetails  = UploadDetails(Instant.now(), "checksum", "fileMimeType", "fileName", 100)
  val readyCallback  = UpscanReadyCallback(Reference("Reference"), new URL("https://callbackUrl.com"), uploadDetails)
  val failedCallback = UpscanFailedCallback(Reference("Reference"), ErrorDetails("failureReason", "message"))

  import models.upscan.UpscanCallback._

  implicit val failedWrites: OWrites[UpscanFailedCallback] = Json
    .writes[UpscanFailedCallback]
    .transform((js: JsValue) => js.as[JsObject] + ("fileStatus" -> JsString("FAILED")))

  implicit val readWrites: OWrites[UpscanReadyCallback] =
    Json.writes[UpscanReadyCallback].transform((js: JsValue) => js.as[JsObject] + ("fileStatus" -> JsString("READY")))

  val upscanInitiateResponse: UpscanInitiateResponse =
    UpscanInitiateResponse(Reference("reference"), "postTarget", formFields = Map.empty[String, String])

  val inProgressUpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", InProgress)
    )
  )

  val notStartedUpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", NotStarted)
    )
  )

  val multipleNotStartedUpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", NotStarted),
      UpscanIds(UploadId("ID1"), "file2", NotStarted)
    )
  )

  val multipleInPrgoressUpscanCsvFilesList = UpscanCsvFilesList(
    ids = List(
      UpscanIds(testUploadId, "file1", InProgress),
      UpscanIds(UploadId("ID1"), "file2", InProgress)
    )
  )

}
