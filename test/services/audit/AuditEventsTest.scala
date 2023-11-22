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

package services.audit

import models.{ErsMetaData, SchemeInfo}
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.ErsTestHelper

import scala.concurrent.Future

class AuditEventsTest
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with ScalaFutures
    with ErsTestHelper {

  val mockAuditConnector: DefaultAuditConnector             = mock[DefaultAuditConnector]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val rsc                                                   = new ErsMetaData(
    new SchemeInfo(
      schemeRef = "testSchemeRef",
      timestamp = new DateTime(),
      schemeId = "testSchemeId",
      taxYear = "testTaxYear",
      schemeName = "testSchemeName",
      schemeType = "testSchemeType"
    ),
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

      val result = testAuditEvent.ersSubmissionAuditEvent(rsc, "bundle").futureValue
      result shouldBe Success
    }
  }

  "eventMap" should {
    "return a valid Map" in {
      val eventMap = Map(
        "schemeRef"  -> "testSchemeRef",
        "schemeId"   -> "testSchemeId",
        "taxYear"    -> "testTaxYear",
        "schemeName" -> "testSchemeName",
        "schemeType" -> "testSchemeType",
        "aoRef"      -> "testAoRef",
        "empRef"     -> "testEmpRef",
        "agentRef"   -> "testAgentRef",
        "sapNumber"  -> "testSapNumber",
        "bundleRed"  -> "bundle"
      )
      testAuditEvent.eventMap(rsc, "bundle") shouldBe eventMap
    }
  }

  "The auditRunTimeError DataEvent" should {
    class TestException(message: String) extends Throwable(message)

    "include the 'CheckingServiceFileProcessingError' auditType" in {
      val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
      val testAuditEvent                            = new AuditEvents(mockAuditConnector)
      val eventCaptor                               = ArgumentCaptor.forClass(classOf[DataEvent])
      val testException: TestException              = new TestException("testErrorMessage")
      val expectedDataEvent: DataEvent              = dataEvent.copy(
        auditType = "RunTimeError",
        detail = Map(
          "ErrorMessage" -> "testErrorMessage",
          "Context"      -> "testContextInfo",
          "sheetName"    -> "testSheetName",
          "StackTrace"   -> testException.getStackTrace.mkString("Array(", ", ", ")")
        )
      )

      testAuditEvent.auditRunTimeError(testException, "testContextInfo", rsc, "testBundle")
      verify(mockAuditConnector, times(1)).sendEvent(eventCaptor.capture())(any(), any())
      expectedDataEvent.auditSource    shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditSource
      expectedDataEvent.auditType      shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].auditType
      expectedDataEvent.detail.head    shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail.head
      expectedDataEvent.detail.take(1) shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail.take(1)
      expectedDataEvent.detail.take(2) shouldBe eventCaptor.getValue.asInstanceOf[DataEvent].detail.take(2)
    }
  }
}
