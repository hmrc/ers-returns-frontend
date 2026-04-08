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

import com.google.inject.Inject
import config.ApplicationConfig
import connectors.UpscanConnector
import models.upscan.{UploadId, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.Logging
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class UpscanService @Inject() (applicationConfig: ApplicationConfig, upscanConnector: UpscanConnector) extends Logging {

  val isSecure: Boolean            = applicationConfig.upscanProtocol == "https"
  lazy val redirectUrlBase: String = applicationConfig.upscanRedirectBase

  private def urlToString(c: Call): String = redirectUrlBase + c.url
  val uploadFileSizeLimit                  = applicationConfig.uploadFileSizeLimit

  def getUpscanFormDataCsv(uploadId: UploadId, schemeRef: String)(implicit
    hc: HeaderCarrier,
    request: RequestHeader
  ): Future[UpscanInitiateResponse] = {
    val callbackUrl = generateCallbackUrl(
      hc.sessionId,
      sessionId => controllers.internal.routes.CsvFileUploadCallbackController.callback(uploadId, schemeRef, sessionId),
      isSecure
    )

    val success = controllers.routes.CsvFileUploadController.success(uploadId)
    logger.info(s"[UpscanService][getUpscanFormDataCsv] success : $uploadId")

    val failure = controllers.routes.CsvFileUploadController.failure()
    logger.info(s"[UpscanService][getUpscanFormDataCsv] failure: $uploadId")

    val upscanInitiateRequest =
      UpscanInitiateRequest(callbackUrl, urlToString(success), urlToString(failure), 1, uploadFileSizeLimit)
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }

  def getUpscanFormDataOds(
    schemeRef: String
  )(implicit hc: HeaderCarrier, request: RequestHeader): Future[UpscanInitiateResponse] = {
    val callbackUrl = generateCallbackUrl(
      hc.sessionId,
      sessionId => controllers.internal.routes.FileUploadCallbackController.callback(schemeRef, sessionId),
      isSecure
    )

    val success               = controllers.routes.FileUploadController.success()
    val failure               = controllers.routes.FileUploadController.failure()
    val upscanInitiateRequest =
      UpscanInitiateRequest(callbackUrl, urlToString(success), urlToString(failure), 1, uploadFileSizeLimit)
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }

  private def generateCallbackUrl(sessionIdOption: Option[SessionId], routeFunction: String => Call, isSecure: Boolean)(
    implicit request: RequestHeader
  ): String =

    sessionIdOption match {
      case Some(sessionId) =>
        routeFunction(sessionId.value).absoluteURL(isSecure)

      case None =>
        val errMsg = "[UpscanService][generateCallbackUrl] Session ID is not available for generating callback URL"
        logger.error(errMsg)
        throw new IllegalStateException(errMsg)
    }

}
