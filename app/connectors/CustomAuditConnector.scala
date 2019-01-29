/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import play.api._

import scala.concurrent.ExecutionContext



 object CustomAuditConnector extends CustomAuditConnector {
  override lazy val auditConnector = AuditServiceConnector
}

trait CustomAuditConnector {

  val auditConnector: AuditConnector

  def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Unit =
    auditConnector.sendEvent(event)
}

object AuditServiceConnector extends AuditConnector with AppName {
  override def appName: String = AppName(Play.current.configuration).appName
  override protected def appNameConfiguration: Configuration = Play.current.configuration
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}
