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
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import views.html.file_size_limit_error

import scala.jdk.CollectionConverters.CollectionHasAsScala

class FileSizeLimitErrorViewSpec extends ViewSpecBase {

  val oneHundredMbInBytes = 104857600

   val application: Application = new GuiceApplicationBuilder()
    .configure("file-size.uploadSizeLimit" -> oneHundredMbInBytes)
    .build()

  implicit val request = FakeRequest("GET", "/foo")
  implicit val messages: Messages = testMessages
  implicit val appConfig: ApplicationConfig = application.injector.instanceOf[ApplicationConfig]

  val view = application.injector.instanceOf[file_size_limit_error]

  "file size limit error view" should {

    "show expected page elements for CSV" in {
      val csvBackLink = controllers.routes.CsvFileUploadController.uploadFilePage().url
      val doc = asDocument(view(csvBackLink))
      hasExpectedPageElements(doc, csvBackLink)
    }

    "show expected page elements for ODS" in {
      val odsBackLink = controllers.routes.FileUploadController.uploadFilePage().url
      val doc: Document = asDocument(view(odsBackLink))
      hasExpectedPageElements(doc, odsBackLink)
    }

    def hasExpectedPageElements(doc: Document, backLinkUrl: String): Unit = {

      doc.title() mustBe "There is a problem – Employment Related Securities – GOV.UK"
      doc.getElementsByClass("govuk-heading-xl").text() mustBe "There is a problem"

      val backLink = doc.getElementsByClass("govuk-back-link")
      backLink.size() mustBe 1
      backLink.first().attr("href") mustBe backLinkUrl

      val paragraphs = doc.getElementsByClass("govuk-body").asScala.toList.map(_.text())

      paragraphs.size mustBe 3
      paragraphs mustBe List(
        "Your file is larger than 104.857MB.",
        "You cannot upload a file that is larger than 104.857MB.",
        "You can email shareschemes@hmrc.gov.uk for help with your submission."
      )

      val shareSchemeLink = doc.getElementsByClass("share-schemes-link")

      shareSchemeLink.size() mustBe 1
      shareSchemeLink.first().attr("href") mustBe "mailto:shareschemes@hmrc.gov.uk"
    }

  }
}
