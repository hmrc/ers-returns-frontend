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

package services.audit

import javax.inject.{Inject, Singleton}
import models.ErsMetaData
import org.apache.commons.lang3.exception.ExceptionUtils
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector

import scala.concurrent.Future

@Singleton
class AuditEvents @Inject()(val auditConnector: DefaultAuditConnector) extends AuditService {

  def auditRunTimeError(exception : Throwable, contextInfo : String, rsc: ErsMetaData, bundle : String)
											 (implicit request: Request[_], hc: HeaderCarrier) : Unit = {
    sendEvent("RunTimeError",
      Map("ErrorMessage" -> exception.getMessage,
        "Context" -> contextInfo,
        "ReturnServiceCache" -> eventMap(rsc, bundle).toString,
        "StackTrace" -> ExceptionUtils.getStackTrace(exception)))
  }

	def ersSubmissionAuditEvent(rsc: ErsMetaData, bundle: String)(implicit hc: HeaderCarrier): Future[AuditResult] = {
		sendEvent("ErsReturnsFrontendSubmission", eventMap(rsc, bundle))
	}

  def eventMap(rsc : ErsMetaData, bundle : String): Map[String,String] = {
    Map(
      "schemeRef" -> rsc.schemeInfo.schemeRef,
      "schemeId" -> rsc.schemeInfo.schemeId,
      "taxYear" -> rsc.schemeInfo.taxYear,
      "schemeName" -> rsc.schemeInfo.schemeName,
      "schemeType" -> rsc.schemeInfo.schemeType,
      "aoRef" -> rsc.aoRef.getOrElse(""),
      "empRef" -> rsc.empRef,
      "agentRef" -> rsc.agentRef.getOrElse(""),
      "sapNumber" -> rsc.sapNumber.getOrElse(""),
      "bundleRed" -> bundle
    )
  }
}
