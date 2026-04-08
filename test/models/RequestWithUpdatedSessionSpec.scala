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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Headers
import play.api.test.FakeRequest

class RequestWithUpdatedSessionSpec extends AnyWordSpec with Matchers {

  "RequestWithUpdatedSession" should {
    val originalRequest = FakeRequest(method = "GET", path = "/test")
      .withHeaders(Headers("Content-Type" -> "text/plain"))
      .withSession("originalSessionKey" -> "originalValue")

    val updatedSessionId = "newSessionId"
    val updatedRequest   = RequestWithUpdatedSession(originalRequest, updatedSessionId)

    "update the session with new sessionId" in {
      updatedRequest.session.get("sessionId")          shouldBe Some(updatedSessionId)
      updatedRequest.session.get("originalSessionKey") shouldBe Some("originalValue")
    }

    "inherit properties from the original request" in {
      updatedRequest.body       shouldBe originalRequest.body
      updatedRequest.connection shouldBe originalRequest.connection
      updatedRequest.method     shouldBe originalRequest.method
      updatedRequest.uri        shouldBe originalRequest.uri
      updatedRequest.version    shouldBe originalRequest.version
      updatedRequest.headers    shouldBe originalRequest.headers
      updatedRequest.attrs      shouldBe originalRequest.attrs
    }
  }

}
