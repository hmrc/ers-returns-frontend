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

import models.{ErsMetaData, SchemeInfo}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AuditEventsTest extends UnitSpec with Matchers with MockitoSugar {

	val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
	implicit val hc: HeaderCarrier = HeaderCarrier()
	implicit val request = FakeRequest()
	val rsc = new ErsMetaData(new SchemeInfo(
		schemeRef = "testSchemeRef",
		timestamp = new DateTime(),
		schemeId = "testSchemeId",
		taxYear = "testTaxYear",
		schemeName = "testSchemeName",
		schemeType = "testSchemeType"),
		ipRef = "testIpRef",
		aoRef = Some("testAoRef"),
		empRef = "testEmpRef",
		agentRef = Some("testAgentRef"),
		sapNumber = Some("testSapNumber")
	)

	val dataEvent: DataEvent = DataEvent(
		auditSource = "ers-returns-frontend",
		auditType = "transactionName",
		tags = Map("test" -> "test"),
		detail = Map("test" -> "details")
	)

	val testAuditEvent = new AuditEvents(mockAuditConnector)

	"ersSubmissionAuditEvent" should {
		"do something" in {
			when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(Success))

			val result = testAuditEvent.ersSubmissionAuditEvent(rsc, "bundle")
			await(result) shouldBe Success
		}
	}

	"eventMap" should {
		"return a valid Map" in {
			val eventMap = Map(
				"schemeRef" -> "testSchemeRef",
				"schemeId" -> "testSchemeId",
				"taxYear" -> "testTaxYear",
				"schemeName" -> "testSchemeName",
				"schemeType" -> "testSchemeType",
				"aoRef" -> "testAoRef",
				"empRef" -> "testEmpRef",
				"agentRef" -> "testAgentRef",
				"sapNumber" -> "testSapNumber",
				"bundleRed" -> "bundle"
			)
			testAuditEvent.eventMap(rsc, "bundle") shouldBe eventMap
		}
	}
}
