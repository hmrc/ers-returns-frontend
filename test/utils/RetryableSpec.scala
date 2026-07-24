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

package utils

import config.ApplicationConfig
import controllers.CsvFileUploadController
import models.SchemeInfo
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logger
import play.api.test.Helpers.await
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration.SECONDS

class RetryableSpec
    extends AnyWordSpecLike
      with Matchers
      with OptionValues
      with MockitoSugar
      with GuiceOneAppPerSuite
      with ErsTestHelper
      with LogCapturing
      with ScalaFutures {

  val retryTestLogger: Logger = Logger(classOf[RetryTest])

  class RetryTest extends Retryable {
    import scala.concurrent.duration._

    override val logger: Logger = retryTestLogger
    val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
    when(mockAppConfig.retryDelay).thenReturn(1.millisecond)

    implicit lazy val actorSystem: ActorSystem = app.actorSystem
    override val appConfig: ApplicationConfig  = mockAppConfig

    trait RetryTestUtil {
      def f: Future[Boolean]
    }

    val retryMock: RetryTestUtil = mock[RetryTestUtil]
  }

  def generateExpectedRetryLogMessages(numberRetryLogs: Int,
                                       appendMaxNumberRetiresMessage: Boolean = false,
                                       schemeRef: String = "NOT DEFINED"): Seq[String] = {
    val retryLogMessages: Seq[String] = (0 until numberRetryLogs).map((retry: Int) =>
      s"[RetryableSpec][withRetry] Retrying call x$retry, schemeRef: $schemeRef"
    )
    if (appendMaxNumberRetiresMessage) {
      retryLogMessages :+ s"[RetryableSpec][withRetry] EXHAUSTED MAX NUMBER OF RETRIES (${numberRetryLogs - 1} times)," +
        s" schemeRef: $schemeRef"
    } else {
      retryLogMessages
    }
  }

    "withRetry" should {

      "return the future data once the predicate has been fulfilled" in new RetryTest {
        when(retryMock.f).thenReturn(Future.successful(true))
        val expectedLogMessage = "[RetryableSpec][withRetry] Retrying call x0, schemeRef: NOT DEFINED"
        withCaptureOfLoggingFrom(retryTestLogger) { captureEvents =>
          val result: Boolean = retryMock.f.withRetry(5, callingFunc = "RetryableSpec")(b => b).futureValue
          assert(result)
          assert(captureEvents.exists(_.getMessage.contains(expectedLogMessage)))
        }

        verify(retryMock, times(1)).f
      }

      "retry if the predicate is not fulfilled" in new RetryTest {
        when(retryMock.f).thenReturn(
          Future.successful(false),
          Future.successful(false),
          Future.successful(true)
        )

        withCaptureOfLoggingFrom(retryTestLogger) { captureEvents =>
          val result: Boolean = retryMock.f.withRetry(5, callingFunc = "RetryableSpec")(b => b).futureValue
          assert(result)
          assert(captureEvents.map(_.getMessage) == generateExpectedRetryLogMessages(3))
        }

        verify(retryMock, times(3)).f
      }

      "retry up to a specified maximum number of times if the predicate is not fulfilled" in new RetryTest {
        val schemeInfo: SchemeInfo = SchemeInfo("XA1100000000000", Instant.now, "1", "2016", "EMI", "EMI")
        when(retryMock.f).thenReturn(
          Future.successful(false),
          Future.successful(false),
          Future.successful(false),
          Future.successful(false),
          Future.successful(true)
        )

        withCaptureOfLoggingFrom(retryTestLogger) { captureEvents =>
          val error: LoopException[Boolean] = intercept[LoopException[Boolean]] {
            await(retryMock.f.withRetry(3, maybeSchemeInfo = Some(schemeInfo), callingFunc = "RetryableSpec")(b => b), 1, SECONDS)
          }
          assert(error == LoopException(3, Some(false)))
          assert(error.getMessage == "Failed to meet predicate after retrying 3 times.")
          assert(captureEvents.map(_.getMessage) == generateExpectedRetryLogMessages(4, schemeRef = "XA1100000000000", appendMaxNumberRetiresMessage = true))
        }

        verify(retryMock, times(3)).f
      }

      "return a LoopException if the predicate is never fulfilled" in new RetryTest {
        when(retryMock.f).thenReturn(Future.successful(false))
        val exception: LoopException[Boolean] = intercept[LoopException[Boolean]] {
          await(retryMock.f.withRetry(1, callingFunc = "RetryableSpec")(b => b), 1, SECONDS)
        }
        exception.finalFutureData shouldBe Some(false)
        exception.retryNumber     shouldBe 1
        exception.getMessage      shouldBe s"Failed to meet predicate after retrying ${exception.retryNumber} times."
        verify(retryMock, times(1)).f
      }
    }
}
