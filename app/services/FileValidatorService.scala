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

package services

import connectors.ErsConnector
import controllers.auth.RequestWithOptionalAuthContext
import models.upscan.{UploadStatus, UploadedSuccessfully}
import play.api.Logging
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileValidatorService @Inject() (ersConnector: ErsConnector)(implicit ec: ExecutionContext) extends Logging {

  def createCallbackRecord(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Int] =
    ersConnector.createCallbackRecord

  def updateCallbackRecord(uploadStatus: UploadStatus, sessionId: String)(implicit hc: HeaderCarrier): Future[Int] =
    ersConnector.updateCallbackRecord(uploadStatus, sessionId)

  def getCallbackRecord(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Option[UploadStatus]] =
    ersConnector.getCallbackRecord

  def getSuccessfulCallbackRecord(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[Option[UploadedSuccessfully]] =
    getCallbackRecord.map {
      _.flatMap {
        case upload: UploadedSuccessfully => Some(upload)
        case _                            => None
      }
    }

}
