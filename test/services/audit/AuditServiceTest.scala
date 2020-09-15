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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector

import scala.concurrent.Future

class AuditServiceTest extends WordSpec with Matchers with MockitoSugar {

	val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
	implicit val hc: HeaderCarrier = HeaderCarrier()

	val dataEvent: DataEvent = DataEvent(
		auditSource = "ers-returns-frontend",
		auditType = "transactionName",
		eventId = "fakeId",
		tags = Map("test" -> "test"),
		detail = Map("test" -> "details")
	)

	//TODO replace these with wiremock type tests?
	"sendEvent" should {
		class TestAuditService(auditResult: AuditResult) extends AuditService {
			override val auditConnector: DefaultAuditConnector = mockAuditConnector
			override def buildEvent(transactionName: String, details: Map[String, String])(implicit hc: HeaderCarrier): DataEvent = dataEvent
			when(auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(auditResult))
		}

		"return a Success" in {
			val testAuditService = new TestAuditService(Success)
			val result = testAuditService.sendEvent("transactionName", Map("test" -> "details"))
			await(result) shouldBe Success
		}

		"return a Disabled" in {
			val testAuditService = new TestAuditService(Disabled)
			val result = testAuditService.sendEvent("transactionName", Map("test" -> "details"))
			await(result) shouldBe Disabled
		}

		"return a Failure" in {
			val testAuditService = new TestAuditService(Failure("it failed"))
			val result = testAuditService.sendEvent("transactionName", Map("test" -> "details"))
			await(result) shouldBe Failure("it failed")
		}
	}

	"buildEvent" should {
		class TestAuditService extends AuditService {
			override val auditConnector: DefaultAuditConnector = mockAuditConnector
			override def generateTags(hc: HeaderCarrier): Map[String, String] = Map("test" -> "test")
		}

		"return a valid DataEvent" in {
			val testAuditService = new TestAuditService
			val result = testAuditService.buildEvent("transactionName", Map("test" -> "details"))
			result.auditSource shouldBe dataEvent.auditSource
			result.auditType shouldBe dataEvent.auditType
			result.detail shouldBe dataEvent.detail
			result.tags shouldBe dataEvent.tags

		}
	}

	"generateTags" should {
		class TestAuditService extends AuditService {override val auditConnector: DefaultAuditConnector = mockAuditConnector}
		val testAuditService = new TestAuditService

		"include the dateTime parameter in the returned Map" in {
			val result = testAuditService.generateTags(HeaderCarrier())
			result.contains("dateTime") shouldBe true
		}
	}
}
