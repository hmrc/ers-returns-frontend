/*
 * Copyright 2025 HM Revenue & Customs
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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.FrontendSessionsRepository
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.MongoSupport
import views.ViewSpecBase

import java.util.UUID


class FrontendSessionsRepositorySpec extends PlaySpec
  with BeforeAndAfterEach
  with MockitoSugar
  with MongoSupport
  with ViewSpecBase
  {
  val mockConfiguration: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val sessionCacheRepo = new
      FrontendSessionsRepository(mongoComponent, mockConfiguration)


  "ERSSessionCacheRepository" when {

    "getAllFromSession" should {
      "return CacheItem with all the data from session" in {

        val sessionId: SessionId = SessionId(UUID.randomUUID().toString)
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(("sessionId", sessionId.toString))

        await(sessionCacheRepo.putSession(DataKey("file-type"), "csv"))
        await(sessionCacheRepo.putSession(DataKey("file-size"), "5MB"))

        val result = sessionCacheRepo.getAllFromSession()

        await(result).map { cacheItem: CacheItem =>
          cacheItem.id mustBe sessionId.toString
          cacheItem.data mustBe JsObject(Seq("file-type" -> JsString("csv"), "file-size" -> JsString("5MB")))
        }
      }

      "return None when there is no data in session" in {
        val sessionId: SessionId = SessionId(UUID.randomUUID().toString)
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest().withSession(("sessionId", sessionId.toString))
        val result = sessionCacheRepo.getAllFromSession()

        await(result) mustBe None
      }
    }

  }
}
