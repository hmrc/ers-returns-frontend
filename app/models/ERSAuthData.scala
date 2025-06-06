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

package models

import config.ApplicationConfig
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}
import uk.gov.hmrc.domain.EmpRef

case class ERSAuthData(
  enrolments: Set[Enrolment],
  affinityGroup: Option[AffinityGroup],
  empRef: EmpRef = EmpRef("", "")
) {

	def getEnrolment(key: String): Option[Enrolment] = enrolments.find(_.key.equalsIgnoreCase(key))
	def isAgent: Boolean = (affinityGroup contains Agent) || getEnrolment("HMRC-AGENT-AGENT").isDefined

	def getDassPortalLink(applicationConfig: ApplicationConfig): String = {
		if (isAgent) {
			s"${applicationConfig.dassGatewayHost}${applicationConfig.dassGatewayAgentPath}"
		} else {
			s"${applicationConfig.dassGatewayOrgLink}/${empRef.value}/${applicationConfig.dassGatewayOrgPath}"
		}
	}
}
