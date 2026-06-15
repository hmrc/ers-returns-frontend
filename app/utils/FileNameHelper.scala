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

import models.SchemeInfo
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesList}
import play.api.i18n.Messages

object FileNameHelper {

  def getFinalFileNames(
    data: UpscanCsvFilesList,
    callbackDataList: List[UploadedSuccessfully],
    invalidFiles: List[UploadedSuccessfully],
    schemeInfo: SchemeInfo,
    ersUtil: ERSUtil
  )(implicit messages: Messages): List[String] = {

    val expectedFileNames: List[String] =
      data.ids
        .map(_.fileId)
        .zip(callbackDataList.reverse.map(_.name))
        .map { case (fileId, uploadedName) =>

          val expectedName =
            ersUtil.getPageElement(
              schemeInfo.schemeId,
              ersUtil.PAGE_CHECK_CSV_FILE,
              fileId + ".file_name"
            )

          val expectedNameCsopV5 =
            ersUtil.getPageElement(
              schemeInfo.schemeId,
              ersUtil.PAGE_CHECK_CSV_FILE,
              fileId + ".file_name.v5"
            )

          if (schemeInfo.schemeType == "CSOP" && uploadedName.contains("V5")) {
            expectedNameCsopV5
          } else {
            expectedName
          }
        }

    val uploadedFileNames: List[String] =
      callbackDataList.map(_.name)

    val isMulitpleFiles = callbackDataList.size > 1

    val remainingExpectedNames =
      if (isMulitpleFiles) uploadedFileNames.filterNot(expectedFileNames.contains)
      else expectedFileNames.filterNot(uploadedFileNames.contains)

    remainingExpectedNames
  }

}
