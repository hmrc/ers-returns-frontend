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
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.ERSUtil
import views.html.file_upload_errors_ods

class FileUploadErrorsOdsViewSpec extends ViewSpecBase with FileUploadFixtures with Matchers {

  private val view = app.injector.instanceOf[file_upload_errors_ods]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  "file_upload_errors_ods view" should {

    "show expected elements when expectedScheme = 'CSOP' and requestScheme = 'EMI'" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("CSOP"))
      val doc = asDocument(view(odsRequestObject, "schemeLink", "CSOP", "EMI"))

      doc.getElementsByClass("govuk-body").get(1).text() mustBe "You chose to submit an end of year return for a CSOP scheme but the file you uploaded is for an EMI scheme."
      doc.getElementsByClass("govuk-body").get(2).text() mustBe "To submit an end of year return for a CSOP scheme you must either:"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(0).text() mustBe "upload a file created from a CSOP template"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(1).text() mustBe "upload a file created by using a CSOP guidance notes"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(0).text() mustBe "try again with a file for a CSOP scheme"
      hasExpectedHeaderConstantElements(doc)
    }

    "show expected elements when expectedScheme = 'SIP' and requestScheme = 'EMI'" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("SIP"))
      val doc = asDocument(view(odsRequestObject, "schemeLink", "SIP", "EMI"))

      doc.getElementsByClass("govuk-body").get(1).text() mustBe "You chose to submit an end of year return for a SIP scheme but the file you uploaded is for an EMI scheme."
      doc.getElementsByClass("govuk-body").get(2).text() mustBe "To submit an end of year return for a SIP scheme you must either:"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(0).text() mustBe "upload a file created from a SIP template"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(1).text() mustBe "upload a file created by using a SIP guidance notes"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(0).text() mustBe "try again with a file for a SIP scheme"
      hasExpectedHeaderConstantElements(doc)
    }

    "show expected elements when expectedScheme = 'SAYE' and requestScheme = 'EMI'" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("SAYE"))
      val doc = asDocument(view(odsRequestObject, "schemeLink", "SAYE", "EMI"))

      doc.getElementsByClass("govuk-body").get(1).text() mustBe "You chose to submit an end of year return for a SAYE scheme but the file you uploaded is for an EMI scheme."
      doc.getElementsByClass("govuk-body").get(2).text() mustBe "To submit an end of year return for a SAYE scheme you must either:"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(0).text() mustBe "upload a file created from a SAYE template"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(1).text() mustBe "upload a file created by using a SAYE guidance notes"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(0).text() mustBe "try again with a file for a SAYE scheme"
      hasExpectedHeaderConstantElements(doc)
    }

    "show expected elements when expectedScheme = 'OTHER' and requestScheme = 'EMI'" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("OTHER"))
      val doc = asDocument(view(odsRequestObject, "schemeLink", "OTHER", "EMI"))

      doc.getElementsByClass("govuk-body").get(1).text() mustBe "You chose to submit an end of year return for an OTHER scheme but the file you uploaded is for an EMI scheme."
      doc.getElementsByClass("govuk-body").get(2).text() mustBe "To submit an end of year return for an OTHER scheme you must either:"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(0).text() mustBe "upload a file created from an OTHER template"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(1).text() mustBe "upload a file created by using an OTHER guidance notes"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(0).text() mustBe "try again with a file for an OTHER scheme"
      hasExpectedHeaderConstantElements(doc)
    }

    "show expected elements when expectedScheme = 'EMI' and requestScheme = 'OTHER'" in {
      val odsRequestObject = testRequestObject.copy(schemeType = Some("EMI"))
      val doc = asDocument(view(odsRequestObject, "schemeLink", "EMI", "OTHER"))

      doc.getElementsByClass("govuk-body").get(1).text() mustBe "You chose to submit an end of year return for an EMI scheme but the file you uploaded is for an OTHER scheme."
      doc.getElementsByClass("govuk-body").get(2).text() mustBe "To submit an end of year return for an EMI scheme you must either:"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(0).text() mustBe "upload a file created from an EMI template"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(0).getElementsByTag("li").get(1).text() mustBe "upload a file created by using an EMI guidance notes"
      doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(0).text() mustBe "try again with a file for an EMI scheme"
      hasExpectedHeaderConstantElements(doc)
    }

  }

  private def hasExpectedHeaderConstantElements(doc: Document): Unit = {
    firstElementByClassText(doc, "govuk-heading-xl") mustBe "There is a problem with the file upload"
    doc.getElementsByClass("govuk-body").get(3).text mustBe "You can:"
    doc.getElementsByClass("govuk-list govuk-list--bullet").get(1).getElementsByTag("li").get(1).text() mustBe "view your schemes and arrangements to choose a different scheme"
  }

}
