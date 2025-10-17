/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

trait AuditService {
  val auditSource = "ers-returns-frontend"
  val auditConnector: DefaultAuditConnector

  def sendEvent(transactionName: String, details: Map[String, String])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[AuditResult] =
    auditConnector.sendEvent(buildEvent(transactionName, details))

  private[audit] def buildEvent(transactionName: String, details: Map[String, String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = auditSource,
      auditType = transactionName,
      tags = generateTags(hc),
      detail = details
    )

  private[audit] def generateTags(hc: HeaderCarrier): Map[String, String] = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val formattedZonedDateTime: String = ZonedDateTime.now().format(formatter)
    hc.otherHeaders.toMap ++ Map("dateTime" -> formattedZonedDateTime)
  }
}
