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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.ERSUtil
import views.html.file_upload_errors

class FileUploadErrorsViewSpec extends ViewSpecBase with FileUploadFixtures with Matchers {

  private val view = app.injector.instanceOf[file_upload_errors]

  implicit val ersUtil: ERSUtil                             = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig                 = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages                           = testMessages

  "file_upload_errors_ods view" should {

    "show expected elements when an invalid file is uploaded" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("CSOP"))
      val doc              = asDocument(view(odsRequestObject, "ODS"))

      hasExpectedContent(
        doc,
        firstParagraph = "You need to correct the errors before you can upload the file.",
        secondParagraph = "You can:",
        firstBullet = "use the file checking service to help correct the errors",
        secondBullet = "try again with a different file",
        schemeRef = "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"
      )
    }

  }

  private def hasExpectedContent(
    doc: Document,
    firstParagraph: String,
    secondParagraph: String,
    firstBullet: String,
    secondBullet: String,
    schemeRef: String
  ): Unit = {
    doc.getElementsByClass("govuk-body").get(1).text() mustBe firstParagraph
    doc.getElementsByClass("govuk-body").get(2).text() mustBe secondParagraph
    doc
      .getElementsByClass("govuk-list govuk-list--bullet")
      .get(0)
      .getElementsByTag("li")
      .get(0)
      .text()                                          mustBe firstBullet
    doc
      .getElementsByClass("govuk-list govuk-list--bullet")
      .get(0)
      .getElementsByTag("li")
      .get(1)
      .text()                                          mustBe secondBullet
    doc.getElementById("scheme-reference").text()      mustBe schemeRef
  }

}
