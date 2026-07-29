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
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import repositories.RateLimiterCache

import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.FiniteDuration

class RateLimiterCacheSpec extends AnyWordSpecLike {

  class RateLimitCacheTest(expiry: FiniteDuration) {
    val rateLimiterCache: RateLimiterCache = new RateLimiterCache(expiry)
  }

  "RateLimiterCache" should {
    "insert and retrieve a key value pair from cache" in new RateLimitCacheTest(
      new FiniteDuration(10, TimeUnit.SECONDS)
    ) {
      rateLimiterCache.insertRateLimiter("123", Instant.now())
      rateLimiterCache.insertRateLimiter("456", Instant.now())
      rateLimiterCache.insertRateLimiter("789", Instant.now())
      assert(rateLimiterCache.getRateLimiter("123").isDefined)
      assert(rateLimiterCache.getRateLimiter("456").isDefined)
      assert(rateLimiterCache.getRateLimiter("789").isDefined)
    }

    "overwrite records which the same key" in new RateLimitCacheTest(new FiniteDuration(10, TimeUnit.SECONDS)) {
      rateLimiterCache.insertRateLimiter("123", Instant.parse("2026-08-04T12:30:00Z"))
      rateLimiterCache.insertRateLimiter("123", Instant.parse("2026-08-04T12:31:00Z"))
      rateLimiterCache.getRateLimiter("123") mustBe Some(Instant.parse("2026-08-04T12:31:00Z"))
    }
  }

  "getRateLimiter" should {

    "return None if there is no rate limit for a given id" in new RateLimitCacheTest(
      new FiniteDuration(10, TimeUnit.SECONDS)
    ) {
      rateLimiterCache.getRateLimiter("123") mustBe None
    }

    "return None only when the rate limiter record has expired" in {

      class CustomTicker extends Ticker {
        private val time = new AtomicLong(0L)

        def advance(nanos: Long): Unit = time.addAndGet(nanos)
        override def read(): Long      = time.get()
      }

      val ticker: CustomTicker = new CustomTicker()

      val rateLimiterCache: RateLimiterCache = new RateLimiterCache(new FiniteDuration(60L, TimeUnit.SECONDS)) {
        override protected val cache: Cache[String, Instant] = Scaffeine()
          .expireAfterWrite(new FiniteDuration(60L, TimeUnit.SECONDS))
          .ticker(ticker)
          .build[String, Instant]()
      }

      val createdAtTime: Instant = Instant.now()
      rateLimiterCache.insertRateLimiter("123", createdAtTime)

      ticker.advance(30.seconds.toNanos) // Advance 30s
      rateLimiterCache.getRateLimiter("123") mustBe Some(createdAtTime)

      ticker.advance(29.seconds.toNanos) // Advance 29s
      rateLimiterCache.getRateLimiter("123") mustBe Some(createdAtTime)

      ticker.advance(1.seconds.toNanos) // Advance 1s
      rateLimiterCache.getRateLimiter("123") mustBe None
    }
  }

}
