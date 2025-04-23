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
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.ERSUtil
import views.html.upscan_ods_file_upload

class UpscanOdsFileUploadViewSpec extends ViewSpecBase with FileUploadFixtures {

  private val view = app.injector.instanceOf[upscan_ods_file_upload]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  "upscan_ods_file_upload view" should {

    "show expected elements for CSOP page" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("CSOP"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"
      firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Company Share Option Plan scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for EMI page" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("EMI"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "EMI - Enterprise Management Incentives scheme - XA1100000000000 - 2014 to 2015"
      firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Enterprise Management Incentives scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for SAYE page" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("SAYE"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "SAYE - Save As You Earn scheme - XA1100000000000 - 2014 to 2015"
      firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Save As You Earn scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for SIP page" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("SIP"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "SIP - Share Incentive Plan scheme - XA1100000000000 - 2014 to 2015"
      firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Share Incentive Plan scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for OTHER page" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("OTHER"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "OTHER - Other scheme - XA1100000000000 - 2014 to 2015"
      firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Other scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

  }

  private def hasExpectedHeaderAndUploadElements(doc: Document): Unit = {
    firstElementByClassText(doc, "govuk-heading-xl") mustBe "Upload your ODS file"
    firstElementByClassText(doc, "govuk-warning-text") mustBe "! Warning You must save a copy of your ODS file for your records."
    firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file"
    doc.getElementsByClass("govuk-file-upload").size() mustBe 1
    doc.title() mustBe "Upload your ODS file – Employment Related Securities – GOV.UK"
    firstElementByClassText(doc, "govuk-button") mustBe "Upload file"
  }

}
