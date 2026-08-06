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

package repository

import scala.concurrent.duration._
import com.github.benmanes.caffeine.cache.Ticker
import com.github.benmanes.caffeine.cache.Ticker.systemTicker
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import config.ApplicationConfig
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.RateLimiterCache
import utils.Fixtures.mock

import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.FiniteDuration

class RateLimiterCacheSpec extends AnyWordSpecLike {

  val staticCreatedAt: Instant = Instant.parse("2026-08-04T12:30:00Z")

  class RateLimitCacheTestSetup(
    expiry: FiniteDuration,
    ticker: Ticker = systemTicker,
    initData: Map[String, Instant] = Map.empty[String, Instant]
  ) {
    val mockAppConfigWithExpiry: ApplicationConfig = mock[ApplicationConfig]
    when(mockAppConfigWithExpiry.confirmationPageRateLimitTTLDuration).thenReturn(expiry)

    val rateLimiterCache: RateLimiterCache = new RateLimiterCache(mockAppConfigWithExpiry) {
      override protected val cache: Cache[String, Instant] = Scaffeine()
        .expireAfterWrite(expiry)
        .ticker(ticker)
        .build[String, Instant]()

      cache.putAll(initData)
    }

  }

  object customTicker extends Ticker {
    private val time = new AtomicLong(0L)

    def advance(nanos: Long): Unit = time.addAndGet(nanos)
    override def read(): Long      = time.get()
  }

  "rateLimitPresentInCache" should {
    "return false if a rate limit with a given id is not in the cache" in new RateLimitCacheTestSetup(
      new FiniteDuration(10, TimeUnit.SECONDS)
    ) {
      rateLimiterCache.rateLimitPresentInCache("123", staticCreatedAt) mustBe false
      rateLimiterCache.rateLimitPresentInCache("456", staticCreatedAt) mustBe false
      rateLimiterCache.rateLimitPresentInCache("789", staticCreatedAt) mustBe false
    }

    "return true if there is already a record with a given id" in new RateLimitCacheTestSetup(
      new FiniteDuration(10, TimeUnit.SECONDS),
      initData = Map(
        ("123", staticCreatedAt)
      )
    ) {
      rateLimiterCache.rateLimitPresentInCache("123", staticCreatedAt) mustBe true
    }

    "return false only when the rate limiter record has expired" in new RateLimitCacheTestSetup(
      new FiniteDuration(60L, TimeUnit.SECONDS),
      customTicker,
      initData = Map(
        ("123", staticCreatedAt)
      )
    ) {
      customTicker.advance(30.seconds.toNanos) // Advance 30s
      rateLimiterCache.rateLimitPresentInCache("123", staticCreatedAt) mustBe true

      customTicker.advance(29.seconds.toNanos) // Advance 29s
      rateLimiterCache.rateLimitPresentInCache("123", staticCreatedAt) mustBe true

      customTicker.advance(1.seconds.toNanos) // Advance 1s
      rateLimiterCache.rateLimitPresentInCache("123", staticCreatedAt) mustBe false
    }
  }

}
