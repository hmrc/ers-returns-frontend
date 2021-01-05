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

package models.upscan

import play.api.libs.json.{Format, Json}

case class UpscanCsvFilesCallback(uploadId: UploadId, fileId: String, uploadStatus: UploadStatus = NotStarted) {
  def isStarted: Boolean = uploadStatus != NotStarted
  def isComplete: Boolean = uploadStatus match {
    case _: UploadedSuccessfully | Failed => true
    case _ => false
  }
}
object UpscanCsvFilesCallback {
  implicit val upscanCsvFileFormats: Format[UpscanCsvFilesCallback] = Json.format[UpscanCsvFilesCallback]
}

case class UpscanCsvFilesCallbackList(files: List[UpscanCsvFilesCallback]){
  def areAllFilesComplete(): Boolean = files.forall(_.isComplete)

  def areAllFilesSuccessful(): Boolean = files.forall {
    _.uploadStatus.isInstanceOf[UploadedSuccessfully]
  }
}
object UpscanCsvFilesCallbackList {
  implicit val upscanCsvCallbackListFormat: Format[UpscanCsvFilesCallbackList] =
    Json.format[UpscanCsvFilesCallbackList]
}

case class UpscanCsvFilesList(ids: List[UpscanIds]) {
  def updateToInProgress(uploadId: UploadId): UpscanCsvFilesList = {
    val notStartedIdExists = ids.exists(id => id.uploadId == uploadId && id.uploadStatus == NotStarted)
    if(notStartedIdExists) {
      val newIds = ids.map {
        case ids@UpscanIds(`uploadId`, _, NotStarted) =>
          ids.copy(uploadStatus = InProgress)
        case other => other
      }
      UpscanCsvFilesList(ids = newIds)
    } else {
      throw new Exception(s"Could not find id ${uploadId.value} in $ids")
    }
  }

  def noOfUploads: Int = ids.count(_.uploadStatus == InProgress)

  def noOfFilesToUpload: Int = ids.size

}
object UpscanCsvFilesList {
  implicit val upscanCsvFilesListFormat: Format[UpscanCsvFilesList] =
    Json.format[UpscanCsvFilesList]
}

case class UpscanIds(uploadId: UploadId, fileId: String, uploadStatus: UploadStatus)
object UpscanIds {
  implicit val upscanIdsFormat: Format[UpscanIds] = Json.format[UpscanIds]
}