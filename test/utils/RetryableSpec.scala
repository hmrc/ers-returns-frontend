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

package utils

import config.ApplicationConfig
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.await

import scala.concurrent.Future
import scala.concurrent.duration.SECONDS

class RetryableSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with GuiceOneAppPerSuite
    with ErsTestHelper {

  class RetryTest extends Retryable {
    import scala.concurrent.duration._
    val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
    when(mockAppConfig.retryDelay).thenReturn(1.millisecond)

    implicit lazy val actorSystem: ActorSystem = app.actorSystem
    override val appConfig: ApplicationConfig  = mockAppConfig
    trait RetryTestUtil {
      def f: Future[Boolean]
    }
    val retryMock: RetryTestUtil               = mock[RetryTestUtil]
  }

  "withRetry" should {
    "return the future data once the predicate has been fulfilled" in new RetryTest {
      when(retryMock.f).thenReturn(Future.successful(true))
      val result: Boolean = await(retryMock.f.withRetry(5)(b => b), 1, SECONDS)
      result shouldBe true
      verify(retryMock, times(1)).f
    }

    "retry if the predicate is not fulfilled" in new RetryTest {
      when(retryMock.f).thenReturn(
        Future.successful(false),
        Future.successful(false),
        Future.successful(true)
      )
      val result: Boolean = await(retryMock.f.withRetry(5)(b => b), 1, SECONDS)
      result shouldBe true
      verify(retryMock, times(3)).f
    }

    "retry up to a specified maximum number of times if the predicate is not fulfilled" in new RetryTest {
      when(retryMock.f).thenReturn(
        Future.successful(false),
        Future.successful(false),
        Future.successful(false),
        Future.successful(false),
        Future.successful(true)
      )
      intercept[Throwable](await(retryMock.f.withRetry(3)(b => b), 1, SECONDS))
      verify(retryMock, times(3)).f
    }

    "return a LoopException if the predicate is never fulfilled" in new RetryTest {
      when(retryMock.f).thenReturn(Future.successful(false))
      val exception: LoopException[Boolean] = intercept[LoopException[Boolean]] {
        await(retryMock.f.withRetry(1)(b => b), 1, SECONDS)
      }
      exception.finalFutureData shouldBe Some(false)
      exception.retryNumber     shouldBe 1
      exception.getMessage      shouldBe s"Failed to meet predicate after retrying ${exception.retryNumber} times."
      verify(retryMock, times(1)).f
    }
  }
}
