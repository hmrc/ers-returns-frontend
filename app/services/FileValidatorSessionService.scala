/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import models.upscan.{NotStarted, UploadStatus, UploadedSuccessfully}
import play.api.Logging
import play.api.mvc.Request
import repositories.FileValidatorSessionsRepository
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileValidatorSessionService @Inject()(sessionCache: FileValidatorSessionsRepository)(implicit ec: ExecutionContext) extends Logging {

  val CALLBACK_DATA_KEY = "callback_data_key"

  def createCallbackRecord(implicit request: Request[_]): Future[(String, String)] =
    sessionCache.putSession[UploadStatus](DataKey(CALLBACK_DATA_KEY), NotStarted)

  def updateCallbackRecord(uploadStatus: UploadStatus)(implicit request: Request[_]) =
    sessionCache.putSession[UploadStatus](DataKey(CALLBACK_DATA_KEY), uploadStatus)

  def getCallbackRecord(implicit request: Request[_]): Future[Option[UploadStatus]] = {
    sessionCache.getFromSession[UploadStatus](DataKey(CALLBACK_DATA_KEY))
  }

  def getSuccessfulCallbackRecord(implicit request: Request[_]): Future[Option[UploadedSuccessfully]] =
    getCallbackRecord.map {
      _.flatMap {
        case upload: UploadedSuccessfully => Some(upload)
        case _ => None
      }
    }
}
