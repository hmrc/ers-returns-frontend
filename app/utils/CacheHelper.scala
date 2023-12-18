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

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Reads}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

trait CacheHelper extends Logging {
  def getEntry[A](cacheItem: CacheItem, key: DataKey[A])(implicit reads: Reads[A]): Option[A] = {
    (cacheItem.data \ key.unwrap).validate[A] match {
      case JsSuccess(value, _) =>
        Some(value)
      case JsError(errors) =>
        val errorDetails = errors.map { case (path, validationErrors) =>
          s"Path: $path, Errors: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")
        logger.warn(s"Error parsing JSON for key ${key.unwrap}: $errorDetails")
        None
    }
  }
}
