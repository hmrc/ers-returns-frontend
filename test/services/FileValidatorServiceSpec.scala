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

import connectors.ErsConnector
import controllers.auth.RequestWithOptionalAuthContext
import models.upscan.{Failed, NotStarted, UploadStatus, UploadedSuccessfully}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{CREATED, NO_CONTENT}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.Fixtures.defaultErsAuthData

import scala.concurrent.{ExecutionContext, Future}

class FileValidatorServiceSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  val sessionId = "sessionId"
  val request: Request[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier().copy(sessionId = Some(SessionId(sessionId)))

  val mockConnector: ErsConnector = mock[ErsConnector]

  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new FileValidatorService(mockConnector)

  implicit val requestWithAuth: RequestWithOptionalAuthContext[AnyContent] =
    RequestWithOptionalAuthContext(request, defaultErsAuthData)

  "createCallbackRecord" must {
    "return CREATED " when {
      "callback record was created successfully" in {
        when(mockConnector.createCallbackRecord(any(), any()))
          .thenReturn(Future.successful(CREATED))

        val result = testService.createCallbackRecord.futureValue

        result shouldBe CREATED
      }
    }

    "throw an exception" when {
      "creating callback record fails" in {
        when(mockConnector.createCallbackRecord(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        an[Exception] should be thrownBy testService.createCallbackRecord.futureValue
      }
    }
  }

  "updateCallbackRecord" must {
    "return NO_CONTENT" when {
      "update was successful" in {
        when(mockConnector.updateCallbackRecord(any(), any())(any()))
          .thenReturn(Future.successful(NO_CONTENT))

        val result = testService.updateCallbackRecord(NotStarted, sessionId).futureValue

        result shouldBe NO_CONTENT
      }
    }

    "throw an exception" when {
      "updating callback fails" in {
        when(mockConnector.updateCallbackRecord(any(), any())(any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        an[Exception] should be thrownBy testService.updateCallbackRecord(Failed, sessionId).futureValue
      }
    }
  }

  "getCallbackRecord" must {
    "return a callback record" when {
      "record is successfully retrieved" in {
        val mockUploadStatus: Option[UploadStatus] = Some(UploadedSuccessfully("fileId", "downloadUrl"))
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.successful(mockUploadStatus))

        val result = testService.getCallbackRecord.futureValue

        result shouldBe mockUploadStatus
      }
    }

    "return None" when {
      "no record is found" in {
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.successful(None))

        val result = testService.getCallbackRecord.futureValue

        result shouldBe None
      }
    }

    "throw an exception" when {
      "retrieving callback record fails" in {
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        an[Exception] should be thrownBy testService.getCallbackRecord.futureValue
      }
    }
  }

  "getSuccessfulCallbackRecord" must {
    "return a UploadedSuccessfully record" when {
      "record is successfully retrieved and is UploadedSuccessfully" in {
        val mockUploadStatus: Option[UploadStatus] = Some(UploadedSuccessfully("fileId", "downloadUrl"))
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.successful(mockUploadStatus))

        val result = testService.getSuccessfulCallbackRecord.futureValue

        result shouldBe Some(UploadedSuccessfully("fileId", "downloadUrl"))
      }
    }

    "return None" when {
      "record is successfully retrieved but is not UploadedSuccessfully" in {
        val mockUploadStatus: Option[UploadStatus] = Some(NotStarted)
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.successful(mockUploadStatus))

        val result = testService.getSuccessfulCallbackRecord.futureValue

        result shouldBe None
      }

      "no record is found" in {
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.successful(None))

        val result = testService.getSuccessfulCallbackRecord.futureValue

        result shouldBe None
      }
    }

    "throw an exception" when {
      "retrieving successful callback record fails" in {
        when(mockConnector.getCallbackRecord(any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        an[Exception] should be thrownBy testService.getSuccessfulCallbackRecord.futureValue
      }
    }
  }
}
