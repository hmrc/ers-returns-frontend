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
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.ERSUtil
import views.html.start

import scala.collection.mutable
import scala.jdk.CollectionConverters._

class StartPageViewSpec extends ViewSpecBase with FileUploadFixtures {

  private val view = app.injector.instanceOf[start]

  implicit val ersUtil: ERSUtil = app.injector.instanceOf[ERSUtil]
  implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages = testMessages

  // for all pages except SIP scheme
  val expectedBullets: List[String] = List(
    "submit your annual return at the end of the tax year",
    "submit a nil return",
    "name",
    "address",
    "Company Registration Number",
    "Corporation Tax reference",
    "to record any events related to the scheme in an ODS or CSV spreadsheet",
    "to know any alterations made to key features of the plan",
    "your completed CSV or ODS file for this tax year",
    "your submission receipt"
  )

  "start view" should {

    "show expected page elements for CSOP scheme" in {

      val expectedParagraphs = List(
        "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015",
        "You can use this service to:",
        "If you have events to report you can get a template and guidance notes to help you to complete your end of year return.",
        "You can check your employment related securities file for formatting errors before you submit your annual return.",
        "For the scheme organiser and any participating group scheme members, you will need to provide the company’s:",
        "Depending on your circumstances, you may also need:",
        "You must keep a record of your employment related securities annual return for your company records. " +
          "You will not be able to get a copy of this from HMRC at a later date.",
        "The records you must keep are:"
      )

      val requestObject = testRequestObject.copy(schemeType = Some("CSOP"))
      val doc = asDocument(view(requestObject))

      hasExpectedContent(
        doc,
        expectedBreadcrumb = "CSOP - Company Share Option Plan scheme - XA1100000000000 - 2014 to 2015",
        expectedCaption = "Company Share Option Plan scheme",
        expectedParagraphs = expectedParagraphs,
        expectedBullets = expectedBullets
      )
    }

    "show expected page elements for EMI scheme" in {

      val expectedParagraphs = List(
        "EMI - Enterprise Management Incentives scheme - XA1100000000000 - 2014 to 2015",
        "You can use this service to:",
        "If you have events to report you can get a template and guidance notes to help you to complete your end of year return.",
        "You can check your employment related securities file for formatting errors before you submit your annual return.",
        "For the employer company and any qualifying subsidiary companies, you will need to provide the company’s:",
        "Depending on your circumstances, you may also need:",
        "You must keep a record of your employment related securities annual return for your company records. " +
          "You will not be able to get a copy of this from HMRC at a later date.",
        "The records you must keep are:"
      )

      val requestObject = testRequestObject.copy(schemeType = Some("EMI"))
      val doc = asDocument(view(requestObject))

      hasExpectedContent(
        doc,
        expectedBreadcrumb = "EMI - Enterprise Management Incentives scheme - XA1100000000000 - 2014 to 2015",
        expectedCaption = "Enterprise Management Incentives scheme",
        expectedParagraphs = expectedParagraphs,
        expectedBullets = expectedBullets
      )
    }

    "show expected page elements for SIP scheme" in {

      val expectedParagraphs = List(
        "SIP - Share Incentive Plan scheme - XA1100000000000 - 2014 to 2015",
        "You can use this service to:",
        "If you have events to report you can get a template and guidance notes to help you to complete your end of year return.",
        "You can check your employment related securities file for formatting errors before you submit your annual return.",
        "For the establishing company and any participating group plan members, you will need to provide the company’s:",
        "You must provide details for at least one trustee. You will need:",
        "Depending on your circumstances, you may also need:",
        "You must keep a record of your employment related securities annual return for your company records. " +
          "You will not be able to get a copy of this from HMRC at a later date.",
        "The records you must keep are:"
      )

      val expectedBullets = List(
        "submit your annual return at the end of the tax year",
        "submit a nil return",
        "name",
        "address",
        "Company Registration Number",
        "Corporation Tax reference",
        "the name of the trustee",
        "the trustee’s address",
        "to record any events related to the scheme in an ODS or CSV spreadsheet",
        "to know any alterations made to key features of the plan",
        "your completed CSV or ODS file for this tax year",
        "your submission receipt"
      )

      val requestObject = testRequestObject.copy(schemeType = Some("SIP"))
      val doc = asDocument(view(requestObject))

      hasExpectedContent(
        doc,
        expectedBreadcrumb = "SIP - Share Incentive Plan scheme - XA1100000000000 - 2014 to 2015",
        expectedCaption = "Share Incentive Plan scheme",
        expectedParagraphs = expectedParagraphs,
        expectedBullets = expectedBullets
      )
    }

    "show expected page elements for SAYE scheme" in {

      val expectedParagraphs = List(
        "SAYE - Save As You Earn scheme - XA1100000000000 - 2014 to 2015",
        "You can use this service to:",
        "If you have events to report you can get a template and guidance notes to help you to complete your end of year return.",
        "You can check your employment related securities file for formatting errors before you submit your annual return.",
        "For the scheme organiser and any participating group scheme members, you will need to provide the company’s:",
        "Depending on your circumstances, you may also need:",
        "You must keep a record of your employment related securities annual return for your company records. " +
          "You will not be able to get a copy of this from HMRC at a later date.",
        "The records you must keep are:"
      )

      val requestObject = testRequestObject.copy(schemeType = Some("SAYE"))
      val doc = asDocument(view(requestObject))

      hasExpectedContent(
        doc,
        expectedBreadcrumb = "SAYE - Save As You Earn scheme - XA1100000000000 - 2014 to 2015",
        expectedCaption = "Save As You Earn Plan scheme",
        expectedParagraphs = expectedParagraphs,
        expectedBullets = expectedBullets
      )
    }

    "show expected page elements for OTHER scheme" in {

      val expectedParagraphs = List(
        "OTHER - Other scheme - XA1100000000000 - 2014 to 2015",
        "You can use this service to:",
        "If you have events to report you can get a template and guidance notes to help you to complete your end of year return.",
        "You can check your employment related securities file for formatting errors before you submit your annual return.",
        "For the establishing company and any participating group scheme members, you will need to provide the company’s:",
        "Depending on your circumstances, you may also need:",
        "You must keep a record of your employment related securities annual return for your company records. " +
          "You will not be able to get a copy of this from HMRC at a later date.",
        "The records you must keep are:"
      )

      val requestObject = testRequestObject.copy(schemeType = Some("OTHER"))
      val doc = asDocument(view(requestObject))

      hasExpectedContent(
        doc,
        expectedBreadcrumb = "OTHER - Other scheme - XA1100000000000 - 2014 to 2015",
        expectedCaption = "Other schemes and arrangements",
        expectedParagraphs = expectedParagraphs,
        expectedBullets = expectedBullets
      )
    }
  }

  private def hasExpectedContent(doc: Document,
                                 expectedBreadcrumb: String,
                                 expectedCaption: String,
                                 expectedParagraphs: List[String],
                                 expectedBullets: List[String]
                                ): Unit = {

    def expectedElements(actualElements: mutable.Buffer[Element], expectedText: List[String]): Unit = {
      actualElements.size mustBe expectedText.size
      actualElements.zip(expectedText).foreach {
        case (element, expected) =>
          element.text mustBe expected
      }
    }

    doc.title() mustBe "Submit your annual return – Employment Related Securities – GOV.UK"
    doc.getElementsByClass("govuk-heading-xl").text() mustBe "Submit your annual return"
    doc.getElementById("scheme-reference").text() mustBe expectedBreadcrumb
    doc.getElementsByClass("govuk-caption-l").text() mustBe expectedCaption

    val paragraphs = doc.getElementsByClass("govuk-body").asScala
    expectedElements(paragraphs, expectedParagraphs)

    val mediumHeadings = doc.getElementsByClass("govuk-heading-m").asScala
    val expectedMediumHeadings = List("Before you start", "What you will need", "After you submit your annual return")
    expectedElements(mediumHeadings, expectedMediumHeadings)

    val bullets = doc.getElementsByClass("govuk-list").asScala.flatMap(list => list.select("li").asScala)
    expectedElements(bullets, expectedBullets)

    val insetText = doc.getElementsByClass("govuk-inset-text").asScala
    val expectedInsetText = List(
      "A company director or company secretary must have approved the return and must agree to any declaration you make in this service.",
      "You can print or save a copy of your submission receipt after you submit your annual return."
    )
    expectedElements(insetText, expectedInsetText)

    val links = doc.getElementById("content").select("a")
    val firstLink = links.get(0).attr("href")
    val secondLink = links.get(1).attr("href")

    firstLink mustBe "https://www.gov.uk/government/collections/employment-related-securities-detailed-information"
    secondLink mustBe "https://www.gov.uk/guidance/spreadsheet-checking-service-employment-related-securities-ers"

    val buttons = doc.getElementsByClass("govuk-button")
    buttons.size() mustBe 1
    buttons.getFirst.text mustBe "Start now"
  }
}
