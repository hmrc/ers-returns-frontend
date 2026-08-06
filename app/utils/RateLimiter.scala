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

import play.api.Logging
import repositories.RateLimiterCache

import java.time.Instant
import scala.concurrent.Future

case object RateLimitedException extends RuntimeException

trait Throttle extends Logging {
  def rateLimiterCache: RateLimiterCache

  def withThrottle[A](
    rateLimitId: String,
    block: => Future[A]
  ): Future[A] =
    if (rateLimiterCache.rateLimitPresentInCache(rateLimitId, Instant.now())) {
      logger.error(
        s"[Throttle][withThrottle] Request failed to acquire a permit at a tps of ${rateLimiterCache.maxTpsForConfirmationPageGet}"
      )
      Future.failed(RateLimitedException)
    } else {
      block
    }

}
