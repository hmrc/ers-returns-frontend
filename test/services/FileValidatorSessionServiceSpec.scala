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

package services

import models.upscan.{Failed, InProgress, NotStarted, UploadStatus}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import repositories.FileValidatorSessionsRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

class FileValidatorSessionServiceSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  val mockSessionCache: FileValidatorSessionsRepository = mock[FileValidatorSessionsRepository]

  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testSessionService = new FileValidatorSessionService(mockSessionCache)

  val sessionId = "sessionId"
  implicit val request: Request[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier().copy(sessionId = Some(SessionId(sessionId)))

  "createCallbackRecord" must {
    "return Unit value" when {
      "cache is successful" in {
        when(mockSessionCache.putSession[UploadStatus](DataKey(any[String]()), meq(NotStarted))(any(), any(), any()))
          .thenReturn(Future.successful((sessionId, "id")))

        noException should be thrownBy testSessionService.createCallbackRecord.futureValue
      }
    }

    "throw an exception" when {
      "caching fails" in {
        when(mockSessionCache.putSession[UploadStatus](DataKey(any[String]()), meq(NotStarted))(any(), any(), any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))

        an[Exception] should be thrownBy testSessionService.createCallbackRecord.futureValue
      }
    }
  }

  "updateCallbackRecord" must {
    "return successful future" when {
      "cache is successful" in {
        when(mockSessionCache.putSession[UploadStatus](DataKey(any[String]()), meq(InProgress))(any(), any(), any()))
          .thenReturn(Future.successful((sessionId, "id")))

        noException should be thrownBy testSessionService.updateCallbackRecord(InProgress).futureValue
      }
    }

    "throw an exception" when {
      "cache fails" in {
        when(mockSessionCache.putSession[UploadStatus](DataKey(any[String]()), meq(Failed))(any(), any(), any()))
          .thenReturn(Future.failed(new Exception("Expected Exception")))

        an[Exception] should be thrownBy testSessionService.updateCallbackRecord(Failed).futureValue
      }
    }
  }
}
