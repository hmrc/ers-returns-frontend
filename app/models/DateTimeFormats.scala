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

package models

import play.api.libs.json.{Format, JsNumber, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

// TODO remove this in 2 years after PR is merged and use only MongoJavatimeFormats from hmrc-mongo-play-json
// date now is kept for 18 months so in 2 years mongo will only contain 1 format for date time can be using MongoJavatimeFormats only
trait DateTimeFormats {

  private val dateTimeRead: Reads[Instant] = Reads { json =>
    // Try MongoDB date format first
    MongoJavatimeFormats.instantReads.reads(json)
      .orElse {
        // Try NumberLong format (epoch milliseconds)
        json.validate[Long].map(Instant.ofEpochMilli)
      }
      .orElse {
        // Fallback to Instant.now if all parsing fails
        play.api.libs.json.JsSuccess(Instant.now)
      }
  }

  private val dateTimeWrite: Writes[Instant] = (o: Instant) => JsNumber(o.toEpochMilli)

  implicit val dateTimeFormats: Format[Instant] = Format(dateTimeRead, dateTimeWrite)
}
