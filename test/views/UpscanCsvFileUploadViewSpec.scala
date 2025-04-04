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
import models.RequestObject
import models.upscan.{Reference, UpscanInitiateResponse}
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.ERSUtil
import views.html.upscan_csv_file_upload

class UpscanCsvFileUploadViewSpec extends ViewSpecBase with UploadFixtures {

  private val view = app.injector.instanceOf[upscan_csv_file_upload]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  "upscan_csv_file_upload view" when {

    "showing the CSOP page" should {

      "show expected elements for CSOP page when fileId is `file0`" in {
        val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("CSOP"))
        val doc = asDocument(view(odsRequestObject, upscanInitiateResponse, "file0", false))

        doc.getElementById("scheme-reference").text() mustBe "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options granted"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsGranted_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file1`" in {
        val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("CSOP"))
        val doc = asDocument(view(odsRequestObject, upscanInitiateResponse, "file1", false))

        doc.getElementById("scheme-reference").text() mustBe "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options released (including exchanges) cancelled or lapsed in year"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsRCL_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file2`" in {
        val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("CSOP"))
        val doc = asDocument(view(odsRequestObject, upscanInitiateResponse, "file2", false))

        doc.getElementById("scheme-reference").text() mustBe "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options and replacement options exercised"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsExercised_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

    }
  }

  def hasExpectedHeaderAndUploadElements(doc: Document): Unit = {
    firstElementByClassText(doc, "govuk-heading-xl") mustBe "Upload your CSV file"
    firstElementByClassText(doc, "govuk-warning-text") mustBe "! Warning You must save a copy of your CSV file for your records."
    doc.getElementsByClass("govuk-file-upload").size() mustBe 1
    firstElementByClassText(doc, "govuk-button") mustBe "Upload file"
  }
}
