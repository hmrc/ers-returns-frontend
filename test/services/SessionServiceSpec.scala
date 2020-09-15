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

package services

import config.ERSFileValidatorSessionCache
import models.upscan.{Failed, InProgress, NotStarted, UploadStatus}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.Future

class SessionServiceSpec extends PlaySpec with OneServerPerSuite with ScalaFutures with MockitoSugar {

  val mockSessionCache: ERSFileValidatorSessionCache = mock[ERSFileValidatorSessionCache]
	val testSessionService = new SessionService(mockSessionCache)

  implicit val request = FakeRequest()
  val sessionId = "sessionId"
  implicit val hc: HeaderCarrier = HeaderCarrier().copy(sessionId = Some(SessionId(sessionId)))

  "createCallbackRecord" must {
    "return Unit value" when {
      "cache is successfull" in {
        when(mockSessionCache.cache[UploadStatus](meq(testSessionService.CALLBACK_DATA_KEY), meq(NotStarted))
          (any(), any[HeaderCarrier], any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map())))
        noException should be thrownBy testSessionService.createCallbackRecord.futureValue
      }
    }

    "throw an exception" when {
      "caching fails" in {
        when(mockSessionCache.cache[UploadStatus](meq(testSessionService.CALLBACK_DATA_KEY), meq(NotStarted))
          (any(), any[HeaderCarrier], any()))
          .thenReturn(Future.failed(new Exception("Expected exception")))
        an [Exception] should be thrownBy testSessionService.createCallbackRecord.futureValue
      }
    }
  }

  "updateCallbackRecord" must {
    "return successful future" when {
      "cache is successful" in {
        val defaultSource = "defaultSource"
        val sessionId = "SessionId"
        val uploadStatus = InProgress
        when(mockSessionCache.defaultSource).thenReturn(defaultSource)
        when(mockSessionCache.cache(meq(defaultSource), meq(sessionId), meq(testSessionService.CALLBACK_DATA_KEY), meq(uploadStatus))(
          any(), any(), any()
        )).thenReturn(Future.successful(CacheMap("sessionValue", Map())))
        noException should be thrownBy testSessionService.updateCallbackRecord(sessionId, uploadStatus).futureValue
      }
    }

    "throw an exception" when {
      "cache fails" in {
        val defaultSource = "defaultSource"
        val sessionId = "SessionId"
        val uploadStatus = Failed
        when(mockSessionCache.defaultSource).thenReturn(defaultSource)
        when(mockSessionCache.cache(meq(defaultSource), meq(sessionId), meq(testSessionService.CALLBACK_DATA_KEY), meq(uploadStatus))(
          any(), any(), any()
        )).thenReturn(Future.failed(new Exception("Expected Exception")))
        an [Exception] should be thrownBy testSessionService.updateCallbackRecord(sessionId, uploadStatus).futureValue
      }
    }
  }
}
