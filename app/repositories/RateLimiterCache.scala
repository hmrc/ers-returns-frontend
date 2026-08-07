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

package repositories

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import config.ApplicationConfig

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

@Singleton
class RateLimiterCache @Inject() (appConfig: ApplicationConfig) {

  val maxTpsForConfirmationPageGet: FiniteDuration = appConfig.confirmationPageRateLimitTTLDuration

  protected val cache: Cache[String, Instant] = Scaffeine()
    .expireAfterWrite(maxTpsForConfirmationPageGet)
    .build[String, Instant]()

  def isRateLimitPresentInCache(rateLimitId: String, createAt: Instant): Boolean =
    cache.underlying.asMap().putIfAbsent(rateLimitId, createAt) != null

}
