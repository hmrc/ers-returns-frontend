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

package views

import config.ApplicationConfig
import controllers.routes
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.ERSUtil

class IncorrectFileNameViewSpec extends ViewSpecBase with FileUploadFixtures {

  private val view = app.injector.instanceOf[views.html.incorrect_file_name]

  val odsRequestObject = testRequestObject.copy(schemeType = Some("CSOP"))

  implicit val ersUtil: ERSUtil                             = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig                 = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages                           = testMessages

  "incorrect file name view" should {

    "show expected page elements" in {
      val doc = asDocument(view(odsRequestObject))

      doc.title()             mustBe messages("ers.incorrect_file_name.title")
      doc.select("h1").text() mustBe messages("ers.incorrect_file_name.heading")

      val bulletItems = doc.select("ul.govuk-list li")
      bulletItems.get(0).text() mustBe messages("ers.incorrect_file_name.bullet_1")
      bulletItems.get(1).text() mustBe messages("ers.incorrect_file_name.bullet_2")

      val paragraphs = doc.select("p.govuk-body")
      paragraphs.get(2).text() must include(messages("ers.incorrect_file_name.paragraph"))

      val tryAgainLink = doc.select("""a[href="/submit-your-ers-annual-return/upload-ods-file"]""")
      tryAgainLink.text() mustBe messages("ers.incorrect_file_name.tryAgain")

      tryAgainLink.attr("href") mustBe routes.FileUploadController.uploadFilePage().url

    }
  }

}
