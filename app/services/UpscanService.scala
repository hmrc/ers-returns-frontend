/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.Inject
import config.ApplicationConfig
import connectors.UpscanConnector
import models.upscan.{UploadId, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.{Environment, Mode}
import play.api.Mode.Mode
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class UpscanService @Inject()(
                             applicationConfig: ApplicationConfig,
                             upscanConnector: UpscanConnector
                             ) {

  val isSecure: Boolean = applicationConfig.upscanProtocol == "https"
  lazy val redirectUrlBase: String = applicationConfig.upscanRedirectBase
  private def urlToString(c: Call): String = redirectUrlBase + c.url
  def getUpscanFormDataCsv(uploadId: UploadId, scRef: String)(implicit hc: HeaderCarrier, request: Request[AnyRef]): Future[UpscanInitiateResponse] = {
    val callback = controllers.routes.CsvFileUploadCallbackController.callback(uploadId, scRef)
      .absoluteURL(isSecure)

    val success = controllers.routes.CsvFileUploadController.success(uploadId)
    val failure = controllers.routes.CsvFileUploadController.failure()
    val upscanInitiateRequest = UpscanInitiateRequest(callback, urlToString(success), urlToString(failure))
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }

  def getUpscanFormDataOds()(implicit hc: HeaderCarrier, request: Request[_]): Future[UpscanInitiateResponse] = {
    val callback = controllers.routes.FileUploadCallbackController.callback(hc.sessionId.get.value)
      .absoluteURL(isSecure)

    val success = controllers.routes.FileUploadController.success()
    val failure = controllers.routes.FileUploadController.failure()
    val upscanInitiateRequest = UpscanInitiateRequest(callback, urlToString(success), urlToString(failure))
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }
}
