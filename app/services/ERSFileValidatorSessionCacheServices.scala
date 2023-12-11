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
import models.cache.CacheMap
import models.upscan.{NotStarted, UploadStatus, UploadedSuccessfully}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import repository.ERSFileValidatorSessionCacheRepository
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ERSFileValidatorSessionCacheServices @Inject()(ersFileValidatorSessionCacheRepository: ERSFileValidatorSessionCacheRepository)(implicit ec: ExecutionContext) {

  val CALCULATION_RESULTS_KEY: String = "calculation_results_key"
  val CALLBACK_DATA_KEY               = "callback_data_key"
  val SCENARIO_KEY                    = "scenario"

def createCallbackRecord(implicit request: Request[_], hc: HeaderCarrier): Future[CacheMap] =
  ersFileValidatorSessionCacheRepository.putInSession[UploadStatus](DataKey(CALLBACK_DATA_KEY), NotStarted)

def updateCallbackRecord(uploadStatus: UploadStatus)(implicit request: Request[_], hc: HeaderCarrier): Future[CacheMap] = {
  ersFileValidatorSessionCacheRepository.putInSession[UploadStatus](DataKey(CALLBACK_DATA_KEY), uploadStatus)
}

  //todo - I still need to get this getCallbackRecord method sorted out
  def getCallbackRecord(implicit hc: HeaderCarrier): Future[Option[UploadStatus]] =
    ersFileValidatorSessionCacheRepository.getAllFromSession[UploadStatus](CALLBACK_DATA_KEY)

  def getSuccessfulCallbackRecord(implicit hc: HeaderCarrier): Future[Option[UploadedSuccessfully]] =
    getCallbackRecord.map {
      _.flatMap {
        case upload: UploadedSuccessfully => Some(upload)
        case _                            => None
      }
    }
}
