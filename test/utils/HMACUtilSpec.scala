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

import config.ApplicationConfig
import models.RequestObject
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utils.Fixtures.mock

class HMACUtilSpec extends AnyWordSpec with Matchers {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  object TestHMACUtil extends HMACUtil {
    override val appConfig: ApplicationConfig = mockAppConfig
  }

  "HMACUtil" should {

    "return true when HMAC validation is disabled" in {
      when(mockAppConfig.hmacOnSwitch).thenReturn(false)

      val request = mock[RequestObject]

      TestHMACUtil.isHmacAndTimestampValid(request) mustBe true
    }

    "return false when HMAC validation is enabled and timestamp is invalid" in {
      when(mockAppConfig.hmacOnSwitch).thenReturn(true)

      val request = mock[RequestObject]
      when(request.getTS).thenReturn("invalid")

      TestHMACUtil.isHmacAndTimestampValid(request) mustBe false
    }

    "return false when HMAC validation is enabled and HMAC is invalid" in {
      when(mockAppConfig.hmacOnSwitch).thenReturn(true)

      val request          = mock[RequestObject]
      val currentTimestamp =
        (System.currentTimeMillis() / 1000).toString

      when(request.getTS).thenReturn(currentTimestamp)
      when(request.concatenateParameters).thenReturn("test")
      when(request.getHMAC).thenReturn("d3Jvbmc=")

      when(mockAppConfig.hmacToken).thenReturn("dGVzdA==")

      TestHMACUtil.isHmacAndTimestampValid(request) mustBe false
    }
  }

}
