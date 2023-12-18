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

package repositories

import config.ApplicationConfig
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.SessionCacheRepository
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

@Singleton
class FileValidatorSessionsRepositoryConfig @Inject()(applicationConfig: ApplicationConfig) {

  def mongoComponent: MongoComponent = MongoComponent(applicationConfig.fileValidatorSessionsUri)

  def ttl: Duration = applicationConfig.fileValidatorSessionsTTL
}

@Singleton
class FileValidatorSessionsRepository @Inject()(val fileValidatorRepositoryConfig: FileValidatorSessionsRepositoryConfig,
                                        timestampSupport: TimestampSupport)
                                       (implicit ec: ExecutionContext) extends SessionCacheRepository(
  mongoComponent = fileValidatorRepositoryConfig.mongoComponent,
  collectionName = "ers-file-validator",
  ttl = fileValidatorRepositoryConfig.ttl,
  timestampSupport = timestampSupport,
  sessionIdKey = SessionKeys.sessionId
)
