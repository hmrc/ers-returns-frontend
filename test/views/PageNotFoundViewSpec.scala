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

package views

import config.ApplicationConfig
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class PageNotFoundViewSpec extends ViewSpecBase with Matchers {

  private val view = app.injector.instanceOf[views.html.page_not_found_template]

  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  "page not found view" should {

    "show expected page elements" in {
      val doc = asDocument(view())

      doc.title() mustBe messages("ers.app_title")
      firstElementByClassOwnText(doc, "govuk-heading-xl") mustBe messages("ers.global.page.not.found.error.heading")

    }
  }


}
