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
import views.html.upscan_ods_file_upload

class UpscanOdsFileUploadViewSpec extends ViewSpecBase {

  private val view = app.injector.instanceOf[upscan_ods_file_upload]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  private val upscanInitiateResponse =
    UpscanInitiateResponse(
      fileReference = Reference("a1a47ace-cf71-4ad7-b2b1-2f22c1097aef"),
      postTarget = "http://localhost:9570/upscan/upload-proxy",
      formFields = Map("not" -> "used")
    )

  private val testOdsRequestObject =
    RequestObject(
      aoRef = Some("123PA12345678"),
      taxYear = Some("2014/15"),
      ersSchemeRef = Some("XA1100000000000"),
      schemeName = Some("MyScheme"),
      schemeType = None,
      agentRef = None,
      empRef = None,
      ts = None,
      hmac = Some("qlQmNGgreJRqJroWUUu0MxLq2oo=")
    )

  "upscan_ods_file_upload view" should {

    "show expected elements for CSOP page" in {
      val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("CSOP"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"
      doc.getElementsByClass("hmrc-caption govuk-caption-xl").first().ownText() mustBe "Company Share Option Plan scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for EMI page" in {
      val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("EMI"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "EMI - Enterprise Management Incentives scheme - XA1100000000000 - 2014 to 2015"
      doc.getElementsByClass("hmrc-caption govuk-caption-xl").first().ownText() mustBe "Enterprise Management Incentives scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for SAYE page" in {
      val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("SAYE"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "SAYE - Save As You Earn scheme - XA1100000000000 - 2014 to 2015"
      doc.getElementsByClass("hmrc-caption govuk-caption-xl").first().ownText() mustBe "Save As You Earn scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for SIP page" in {
      val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("SIP"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "SIP - Share Incentive Plan scheme - XA1100000000000 - 2014 to 2015"
      doc.getElementsByClass("hmrc-caption govuk-caption-xl").first().ownText() mustBe "Share Incentive Plan scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    "show expected elements for OTHER page" in {
      val odsRequestObject = testOdsRequestObject.copy(schemeType = Some("Other"))
      val doc = asDocument(view(odsRequestObject, upscanInitiateResponse))

      doc.getElementById("scheme-reference").text() mustBe "OTHER - Other scheme - XA1100000000000 - 2014 to 2015"
      doc.getElementsByClass("hmrc-caption govuk-caption-xl").first().ownText() mustBe "Other scheme"
      hasExpectedHeaderAndUploadElements(doc)
    }

    def hasExpectedHeaderAndUploadElements(doc: Document): Unit = {
      doc.getElementsByClass("govuk-heading-xl").first().text() mustBe "Upload your ODS file"
      doc.getElementsByClass("govuk-warning-text").first().text() mustBe "! Warning You must save a copy of your ODS file for your records."
      doc.getElementsByClass("govuk-form-group").first().text() mustBe "Upload a file"
      doc.getElementsByClass("govuk-file-upload").size() mustBe 1
      doc.getElementsByClass("govuk-button").text() mustBe "Upload file"
    }

  }

}
