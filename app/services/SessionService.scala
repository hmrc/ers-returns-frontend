/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ERSFileValidatorSessionCache
import models.upscan.{NotStarted, UploadStatus, UploadedSuccessfully}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class SessionService @Inject()(sessionCache: ERSFileValidatorSessionCache)(implicit ec: ExecutionContext) {

  val CALCULATION_RESULTS_KEY: String = "calculation_results_key"
  val CALLBACK_DATA_KEY = "callback_data_key"
  val SCENARIO_KEY = "scenario"

  def createCallbackRecord(implicit hc: HeaderCarrier): Future[Any] = {
    sessionCache.cache[UploadStatus](CALLBACK_DATA_KEY, NotStarted)
  }

  def updateCallbackRecord(sessionId: String, uploadStatus: UploadStatus)(implicit hc: HeaderCarrier): Future[Any] =
    sessionCache.cache(sessionCache.defaultSource, sessionId, CALLBACK_DATA_KEY, uploadStatus)

  def getCallbackRecord(implicit hc: HeaderCarrier): Future[Option[UploadStatus]] =
    sessionCache.fetchAndGetEntry[UploadStatus](CALLBACK_DATA_KEY)

  def getSuccessfulCallbackRecord(implicit hc: HeaderCarrier): Future[Option[UploadedSuccessfully]] =
    getCallbackRecord.map {
      _.flatMap {
        case upload: UploadedSuccessfully => Some(upload)
        case _ => None
      }
    }
}
