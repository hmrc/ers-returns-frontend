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

import models.ReportableEvents
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json, Reads}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import java.time.Instant

class CacheHelperSpec extends PlaySpec with MockitoSugar {

  object CacheHelperTest extends CacheHelper

  "getEntry" must {
    "return Some(value) when JSON validation is successful" in {
      val expectedValue                           = ReportableEvents(Some("2"))
      val key                                     = DataKey[ReportableEvents]("ReportableEvents")
      val cacheItem                               =
        CacheItem("id", Json.obj("ReportableEvents" -> Json.toJson(expectedValue)), Instant.now(), Instant.now())
      implicit val reads: Reads[ReportableEvents] = Json.reads[ReportableEvents]

      val result = CacheHelperTest.getEntry[ReportableEvents](cacheItem, key)

      result mustBe Some(expectedValue)
    }
  }

  it must {
    "return None when JSON validation fails" in {
      val invalidJson                             = Json.obj("ReportableEvents" -> JsString("invalidData"))
      val key                                     = DataKey[ReportableEvents]("ReportableEvents")
      val cacheItem                               = CacheItem("id", invalidJson, Instant.now(), Instant.now())
      implicit val reads: Reads[ReportableEvents] = Json.reads[ReportableEvents]

      val result = CacheHelperTest.getEntry[ReportableEvents](cacheItem, key)

      result mustBe None
    }
  }

}
