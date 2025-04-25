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
import views.html.confirmation

class ConfirmationViewSpec extends ViewSpecBase with FileUploadFixtures {

  private val view = app.injector.instanceOf[confirmation]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  "confirmation view" should {
    "show expected elements for CSOP page" in {
      val requestObjectWithCsopScheme = testRequestObject.copy(schemeType = Some("CSOP"))
      val doc = asDocument(view(requestObjectWithCsopScheme, "8 April 2016, 4:50pm", "", "2014/15", ""))
      hasExpectedElements(doc, schemeRef = "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015")
    }

    "show expected elements for OTHER page" in {
      val requestObjectWithCsopScheme = testRequestObject.copy(schemeType = Some("OTHER"))
      val doc = asDocument(view(requestObjectWithCsopScheme, "8 April 2016, 4:50pm", "", "2014/15", ""))
      hasExpectedElements(doc, schemeRef = "OTHER - Other scheme - XA1100000000000 - 2014 to 2015")
    }

    "show expected elements for EMI page" in {
      val requestObjectWithCsopScheme = testRequestObject.copy(schemeType = Some("EMI"))
      val doc = asDocument(view(requestObjectWithCsopScheme, "8 April 2016, 4:50pm", "", "2014/15", ""))
      hasExpectedElements(doc, schemeRef = "EMI - Enterprise Management Incentives scheme - XA1100000000000 - 2014 to 2015")
    }

    "show expected elements for SIP page" in {
      val requestObjectWithCsopScheme = testRequestObject.copy(schemeType = Some("SIP"))
      val doc = asDocument(view(requestObjectWithCsopScheme, "8 April 2016, 4:50pm", "", "2014/15", ""))
      hasExpectedElements(doc, schemeRef = "SIP - Share Incentive Plan scheme - XA1100000000000 - 2014 to 2015")
    }

    "show expected elements for SAYE page" in {
      val requestObjectWithCsopScheme = testRequestObject.copy(schemeType = Some("SAYE"))
      val doc = asDocument(view(requestObjectWithCsopScheme, "8 April 2016, 4:50pm", "", "2014/15", ""))
      hasExpectedElements(doc, schemeRef = "SAYE - Save As You Earn scheme - XA1100000000000 - 2014 to 2015")
    }
  }

  private def hasExpectedElements(doc: Document, schemeRef: String): Unit = {
    firstElementByClassText(doc, "govuk-panel__title") mustBe "You have submitted your annual return"
    firstElementByClassText(doc, "govuk-heading-m") mustBe "What you must do next"
    doc.getElementsByClass("govuk-body").get(1).text mustBe "You must keep a copy of your Employment Related Securities annual return for your company records."
    doc.getElementsByClass("govuk-body").get(2).text mustBe "You must keep both of the following:"
    doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(0).text mustBe "your completed CSV or ODS file"
    doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(1).text mustBe "your submission receipt"
    firstElementByClassText(doc, "govuk-inset-text") mustBe "You will not be able to get a copy of your records from HMRC at a later date."
    doc.getElementsByClass("govuk-body").get(3).text mustBe "You need to:"
    doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(0).text mustBe "print or save a copy of your submission receipt (opens in a new tab)"
    doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(1).text mustBe "save your completed ODS or CSV file"
    doc.getElementsByClass("govuk-body").get(4).text mustBe "Allow 48 hours before you check the status of your submission."
    doc.getElementsByClass("govuk-body").get(5).text mustBe "You can view your schemes and arrangements to submit a different annual return."
    doc.getElementsByClass("govuk-body").get(6).text mustBe "What did you think of this service? (takes 30 seconds)"
    doc.getElementsByClass("govuk-body").get(6).text must include ("(takes 30 seconds)")
    doc.getElementById("scheme-reference").text() mustBe schemeRef
  }
}
