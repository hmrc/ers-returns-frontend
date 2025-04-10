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
import views.html.upscan_csv_file_upload

class UpscanCsvFileUploadViewSpec extends ViewSpecBase with FileUploadFixtures {

  private val view = app.injector.instanceOf[upscan_csv_file_upload]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  "upscan_csv_file_upload view" when {

    "showing the CSOP page" should {

      val requestObjectWithCsopScheme = testRequestObject.copy(schemeType = Some("CSOP"))
      val expectedSchemeReference = "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015"

      "show expected elements for CSOP page when fileId is `file0` and useCsopV5Templates is false" in {
        val doc = asDocument(view(requestObjectWithCsopScheme, upscanInitiateResponse, "file0", useCsopV5Templates = false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options granted"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsGranted_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file0` and useCsopV5Templates is true" in {
        val doc = asDocument(view(requestObjectWithCsopScheme, upscanInitiateResponse, "file0", useCsopV5Templates = true))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options granted"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsGranted_V5.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file1` and useCsopV5Templates is false" in {
        val doc = asDocument(view(requestObjectWithCsopScheme, upscanInitiateResponse, "file1", useCsopV5Templates = false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options released (including exchanges) cancelled or lapsed in year"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsRCL_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file1` and useCsopV5Templates is true" in {
        val doc = asDocument(view(requestObjectWithCsopScheme, upscanInitiateResponse, "file1", useCsopV5Templates = true))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options released (including exchanges) cancelled or lapsed in year"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsRCL_V5.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file2` and useCsopV5Templates is false" in {
        val doc = asDocument(view(requestObjectWithCsopScheme, upscanInitiateResponse, "file2", useCsopV5Templates = false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options and replacement options exercised"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsExercised_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for CSOP page when fileId is `file2` and useCsopV5Templates is true" in {
        val doc = asDocument(view(requestObjectWithCsopScheme, upscanInitiateResponse, "file2", useCsopV5Templates = true))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options and replacement options exercised"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file CSOP_OptionsExercised_V5.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

    }

    "showing the EMI page" should {

      val requestObjectWithEmiScheme = testRequestObject.copy(schemeType = Some("EMI"))
      val expectedSchemeReference = "EMI - Enterprise Management Incentives scheme - XA1100000000000 - 2014 to 2015"

      "show expected elements for EMI page when fileId is `file0`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file0", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Adjustment of options"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file EMI40_Adjustments_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for EMI page when fileId is `file1`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file1", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Replacement of options"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file EMI40_Replaced_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for EMI page when fileId is `file2`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file2", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options released, lapsed or cancelled"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file EMI40_RLC_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for EMI page when fileId is `file3`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file3", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Non-taxable exercise of options"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file EMI40_NonTaxable_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }


      "show expected elements for EMI page when fileId is `file4`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file4", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Taxable exercise of options"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file EMI40_Taxable_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }
    }

    "showing the SIP page" should {

      val requestObjectWithEmiScheme = testRequestObject.copy(schemeType = Some("SIP"))
      val expectedSchemeReference = "SIP - Share Incentive Plan scheme - XA1100000000000 - 2014 to 2015"

      "show expected elements for SIP page when fileId is `file0`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file0", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Share awards"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file SIP_Awards_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for SIP page when fileId is `file1`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file1", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Shares withdrawn from the plan"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file SIP_Out_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

    }

    "showing the SAYE page" should {

      val requestObjectWithEmiScheme = testRequestObject.copy(schemeType = Some("SAYE"))
      val expectedSchemeReference = "SAYE - Save As You Earn scheme - XA1100000000000 - 2014 to 2015"

      "show expected elements for SAYE page when fileId is `file0`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file0", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options granted"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file SAYE_Granted_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for SAYE page when fileId is `file1`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file1", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options released (including exchanges), cancelled or lapsed in year"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file SAYE_RCL_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for SAYE page when fileId is `file2`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file2", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Options exercised"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file SAYE_Exercised_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

    }

    "showing the Other page" should {

      val requestObjectWithEmiScheme = testRequestObject.copy(schemeType = Some("OTHER"))
      val expectedSchemeReference = "OTHER - Other scheme - XA1100000000000 - 2014 to 2015"

      "show expected elements for Other page when fileId is `file0`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file0", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Grant of options"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Grants_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file1`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file1", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Other option events"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Options_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file2`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file2", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Acquisition of securities"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Acquisition_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file3`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file3", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Restricted securities post-acquisition events"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_RestrictedSecurities_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file4`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file4", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Receipt of other benefits from securities post-acquisition"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_OtherBenefits_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file5`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file5", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Convertible securities post-acquisition"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Convertible_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file6`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file6", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Discharge of notional loans post-acquisition"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Notional_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file7`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file7", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Artificial enhancement of market value. Value of securities post-acquisition"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Enhancement_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }

      "show expected elements for Other page when fileId is `file8`" in {
        val doc = asDocument(view(requestObjectWithEmiScheme, upscanInitiateResponse, "file8", false))

        doc.getElementById("scheme-reference").text() mustBe expectedSchemeReference
        firstElementByClassOwnText(doc, "hmrc-caption govuk-caption-xl") mustBe "Securities sold for more than market value post-acquisition"
        firstElementByClassText(doc, "govuk-form-group") mustBe "Upload the file Other_Sold_V4.csv"
        hasExpectedHeaderAndUploadElements(doc)
      }
    }

  }

  private def hasExpectedHeaderAndUploadElements(doc: Document): Unit = {
    firstElementByClassText(doc, "govuk-heading-xl") mustBe "Upload your CSV file"
    firstElementByClassText(doc, "govuk-warning-text") mustBe "! Warning You must save a copy of your CSV file for your records."
    doc.getElementsByClass("govuk-file-upload").size() mustBe 1
    doc.title() mustBe "Upload your CSV file – Employment Related Securities – GOV.UK"
    firstElementByClassText(doc, "govuk-button") mustBe "Upload file"
  }
}
