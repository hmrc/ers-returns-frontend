/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import akka.stream.Materializer
import controllers._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

class PageBuilderTest extends UnitSpec with ERSFakeApplicationConfig with MockitoSugar with OneAppPerSuite {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer

	class TestPageBuilder extends PageBuilder

  "calling getPageElement for CSOP scheme pages" should {
    "return the correct page content value" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_START
      val element = "page_title"
      val result: String = getPageElement(scheme, pageId, element)
      result shouldBe Messages("ers_start.csop.page_title")
    }
  }

  "calling getPageElement for EMI scheme pages" should {
    "return the correct page content value" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_START
      val element = "page_title"
      val result: String = getPageElement(scheme, pageId, element)
      result shouldBe Messages("ers_start.emi.page_title")
    }
  }

  "calling getPageElement for SAYE scheme pages" should {
    "return the correct page content value" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_START
      val element = "page_title"
      val result: String = getPageElement(scheme, pageId, element)
      result shouldBe Messages("ers_start.saye.page_title")
    }
  }

  "calling getPageElement for SIP scheme pages" should {
    "return the correct page content value" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_START
      val element: String = "page_title"
      val result: String = getPageElement(scheme, pageId, element)
      result shouldBe Messages("ers_start.sip.page_title")
    }
  }

  "calling getPageElement for OTHER scheme pages" should {
    "return the correct page content value" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_START
      val element: String = "page_title"
      val result: String = getPageElement(scheme, pageId, element)
      result shouldBe Messages("ers_start.other.page_title")
    }
  }

  "calling getPageElement for an invalid scheme pages" should {
    "return the correct page content value" in new TestPageBuilder {
      val scheme: String = "0"
      val pageId: String = PAGE_START
      val element: String = "page_title"
      val result: String = getPageElement(scheme, pageId, element)
      result shouldBe Messages(DEFAULT)
    }
  }


  "calling getPageBackLink for CSOP" should {

    "return the correct back link for placement on scheme organiser page (Nil Return)" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.ReportableEventsController.reportableEventsPage().toString
    }

    "return the correct back link for placement on scheme organiser page (CSV file submission)" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId, condition = OPTION_CSV, reportableEvents = OPTION_UPLOAD_SPREEDSHEET)
      result shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "return the correct back link for placement on scheme organiser page (ODS file submission)" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId, condition = OPTION_ODS, reportableEvents = OPTION_UPLOAD_SPREEDSHEET)
      result shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "return the correct back link for placement on alteration amends activity page, is group scheme NO" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_ALT_ACTIVITY
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "return the correct back link for placement on alteration amends activity page, is group scheme YES" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_ALT_ACTIVITY
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupPlanSummaryPage().toString
    }

    "return the correct back link for placement on alteration amends page" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_ALT_AMENDS
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.AltAmendsController.altActivityPage().toString
    }

    "return the correct back link for placement on group scheme page" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_GROUP_SUMMARY
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.GroupSchemeController.manualCompanyDetailsPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity NO" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.AltAmendsController.altActivityPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity YES" in new TestPageBuilder {
      val scheme: String = SCHEME_CSOP
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.AltAmendsController.altAmendsPage().toString
    }

  }

  "calling getPageBackLink for EMI" should {

    "return the correct back link for placement on scheme organiser page, reportable events YES (ODS File)" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_ODS
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events YES (CSV File)" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_CSV
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events NO" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.ReportableEventsController.reportableEventsPage().toString
    }

    "return the correct back link for placement on group scheme summary page" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_GROUP_SUMMARY
      val condition: String = OPTION_MANUAL
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.manualCompanyDetailsPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity NO" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity YES" in new TestPageBuilder {
      val scheme: String = SCHEME_EMI
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupPlanSummaryPage().toString
    }
  }

  "calling getPageBackLink for SAYE" should {

    "return the correct back link for placement on scheme organiser page, reportable events YES (ODS File)" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_ODS
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events YES (CSV File)" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_CSV
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "return the correct back link for placement on alteration amends activity page, is group scheme NO" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_ALT_ACTIVITY
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "return the correct back link for placement on alteration amends activity page, is group scheme YES" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_ALT_ACTIVITY
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupPlanSummaryPage().toString
    }

    "return the correct back link for placement on alteration amends page" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_ALT_AMENDS
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.AltAmendsController.altActivityPage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events NO" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.ReportableEventsController.reportableEventsPage().toString
    }

    "return the correct back link for placement on group scheme summary page" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_GROUP_SUMMARY
      val condition: String = OPTION_MANUAL
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.manualCompanyDetailsPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity NO" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity YES" in new TestPageBuilder {
      val scheme: String = SCHEME_SAYE
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.AltAmendsController.altAmendsPage().toString
    }

  }


  "calling getPageBackLink for OTHER" should {

    "return the correct back link for placement on scheme organiser page, reportable events YES (ODS File)" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_ODS
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events YES (CSV File)" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_CSV
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events NO" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.ReportableEventsController.reportableEventsPage().toString
    }

    "return the correct back link for placement on group scheme summary page" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_GROUP_SUMMARY
      val condition: String = OPTION_MANUAL
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.manualCompanyDetailsPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity NO" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity YES" in new TestPageBuilder {
      val scheme: String = SCHEME_OTHER
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupPlanSummaryPage().toString
    }

  }

  "calling getPageBackLink for SIP" should {

    "return the correct back link for placement on scheme organiser page, reportable events YES (ODS File)" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_ODS
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.FileUploadController.uploadFilePage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events YES (CSV File)" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_SCHEME_ORGANISER
      val condition: String = OPTION_CSV
      val reportableEvents: String = OPTION_UPLOAD_SPREEDSHEET
      val result: String = getPageBackLink(scheme, pageId, condition, reportableEvents)
      result shouldBe routes.CheckCsvFilesController.checkCsvFilesPage().toString
    }

    "return the correct back link for placement on scheme organiser page, reportable events NO" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_SCHEME_ORGANISER
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.ReportableEventsController.reportableEventsPage().toString
    }

    "return the correct back link for placement on alteration amends activity page" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_ALT_ACTIVITY
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.TrusteeController.trusteeSummaryPage().toString
    }

    "return the correct back link for placement on alteration amends page" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_ALT_AMENDS
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.AltAmendsController.altActivityPage().toString
    }

    "return the correct back link for placement on group scheme page" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_GROUP_SUMMARY
      val result: String = getPageBackLink(scheme, pageId)
      result shouldBe routes.GroupSchemeController.manualCompanyDetailsPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity NO" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.AltAmendsController.altActivityPage().toString
    }

    "return the correct back link for placement on group scheme page, alteration activity YES" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_SUMMARY_DECLARATION
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.AltAmendsController.altAmendsPage().toString
    }

    "return the correct back link for placement on trustee page, group plan NO" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_TRUSTEE_DETAILS
      val condition: String = OPTION_NO
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupSchemePage().toString
    }

    "return the correct back link for placement on trustee page, group plan YES" in new TestPageBuilder {
      val scheme: String = SCHEME_SIP
      val pageId: String = PAGE_TRUSTEE_DETAILS
      val condition: String = OPTION_YES
      val result: String = getPageBackLink(scheme, pageId, condition)
      result shouldBe routes.GroupSchemeController.groupPlanSummaryPage().toString
    }
  }


}
