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

package repository

import config.ApplicationConfig
import models.cache.CacheMap
import org.mongodb.scala.model.Filters
import play.api.libs.json.Writes
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.mongo.cache.{DataKey, SessionCacheRepository}

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class ERSFileValidatorSessionCacheRepository @Inject()(mongoComponent: MongoComponent,
                                                       appConfig: ApplicationConfig,
                                                       timestampSupport: TimestampSupport
                                                      )(implicit ec: ExecutionContext) extends SessionCacheRepository(
  mongoComponent = mongoComponent,
  collectionName = appConfig.appName + "ersFileValidatorSessionCacheRepository",
  ttl = Duration(appConfig.mongoTTLInSeconds, TimeUnit.SECONDS),
  timestampSupport = timestampSupport,
  sessionIdKey = SessionKeys.sessionId
)(ec) {

  def putInSession[T: Writes](dataKey: DataKey[T], data: T)
                             (implicit request: Request[_], ec: ExecutionContext, hc: HeaderCarrier): Future[CacheMap] = {
    cacheRepo
      .put[T](request)(dataKey, data)
      .map(res => CacheMap(res.id, res.data.value.toMap))
  }

  def getAllFromSession(implicit request: Request[_]): Future[Option[CacheMap]] = {
    cacheRepo.findById(request).map(_.map(res => CacheMap(res.id, res.data.value.toMap)))
  }

  def getSession(sessionId: String): Future[Option[CacheMap]] =
    cacheRepo
      .collection
      .find(Filters.equal("_id", sessionId))
      .toFuture()
      .map(_.headOption
        .map(ci => CacheMap(ci.id, ci.data.value.toMap))
      )

  def clear(implicit request: Request[_]): Future[Unit] =
    cacheRepo.deleteEntity(request)

}