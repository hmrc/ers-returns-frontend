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

package controllers.internal

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import services.audit.AuditEvents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global

class FileSizeUtilsSpec extends AnyWordSpecLike with Matchers with MockitoSugar {

  val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
  val mockAuditEvents: AuditEvents              = new AuditEvents(mockAuditConnector)
  implicit val hc: HeaderCarrier                = HeaderCarrier()
  val fileSizeUtils: FileSizeUtils              = new FileSizeUtils(mockAuditEvents)

  "logFileSize" should {
    "audit the file size with the scheme ref" in {
      fileSizeUtils.logFileSize("1234", 100)
      val eventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(mockAuditConnector, times(1)).sendEvent(eventCaptor.capture())(any(), any())
      eventCaptor.getValue.asInstanceOf[DataEvent].auditSource shouldBe "ers-returns-frontend"
      eventCaptor.getValue.asInstanceOf[DataEvent].auditType   shouldBe "UploadFileSizeFromUpscanCallback"
      eventCaptor.getValue.asInstanceOf[DataEvent].detail      shouldBe Map("schemeRef" -> "1234", "fileSize" -> "100")
    }
  }

  "mapFileSizeToString" should {
    val fileSizeInBytesAndExpectedString: Seq[(Int, String)] = Seq(
      (10, "10.00 bytes"),
      (99, "99.00 bytes"),
      (100, "100.00 bytes"),
      (1000, "1000.00 bytes"),
      (1024, "1.00 KB (1024 bytes)"),
      (1126, "1.10 KB (1126 bytes)"),
      (524288, "512.00 KB (524288 bytes)"),
      (1048575, "1024.00 KB (1048575 bytes)"),
      (1048576, "1.00 MB (1048576 bytes)"),
      (1153433, "1.10 MB (1153433 bytes)"),
      (107374182, "102.40 MB (107374182 bytes)"),
      (1073741823, "1024.00 MB (1073741823 bytes)"),
      (1073741824, "1.00 GB (1073741824 bytes)"),
      (1181116006, "1.10 GB (1181116006 bytes)")
    )

    fileSizeInBytesAndExpectedString.foreach { fileSizeAndExpectedString: (Int, String) =>
      s"return the expected string: ${fileSizeAndExpectedString._2} when parsed a file size of: ${fileSizeAndExpectedString._1}" in {
        fileSizeUtils.mapFileSizeToString(fileSizeAndExpectedString._1) shouldBe fileSizeAndExpectedString._2
      }
    }
  }

}
