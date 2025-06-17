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

package repositories

import config.ApplicationConfig
import play.api.mvc.Request
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{CacheItem, SessionCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FrontendSessionsRepository @Inject()(mongoComponent: MongoComponent,
                                           applicationConfig: ApplicationConfig)
                                          (implicit ec: ExecutionContext) extends SessionCacheRepository(
  mongoComponent = mongoComponent,
  collectionName = "sessions",
  ttl = applicationConfig.userSessionsTTL,
  timestampSupport = new CurrentTimestampSupport,
  sessionIdKey = SessionKeys.sessionId
) {
  def getAllFromSession()(implicit request: RequestHeader): Future[Option[CacheItem]] =
    cacheRepo.findById(request)
}
