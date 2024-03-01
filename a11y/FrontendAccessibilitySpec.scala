/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ApplicationConfig
import models._
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.mvc.{AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import utils.{CountryCodes, CountryCodesImpl, ERSUtil}
import views.html._

class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  private val booleanForm: Form[Boolean] = Form("value" -> boolean)

  implicit val arbitraryRequest: Arbitrary[Request[AnyRef]] = fixed(fakeRequest)
  implicit val arbConfig: Arbitrary[ApplicationConfig] = fixed(app.injector.instanceOf[ApplicationConfig])
  implicit val arbErsUtil: Arbitrary[ERSUtil] = fixed(app.injector.instanceOf[ERSUtil])
  implicit val arbRequestObject: Arbitrary[RequestObject] = fixed(RequestObject(Some("aoRef"), Some("2014/15"),
    Some("AA0000000000000"), Some("MyScheme"), Some("CSOP"), Some("agentRef"), Some("empRef"), Some("ts"), Some("hmac")))
  implicit val arbCsvFilesList: Arbitrary[CsvFilesList] = fixed(CsvFilesList(List(CsvFiles("1"), CsvFiles("2"), CsvFiles("3"))))
  implicit val arbHtml: Arbitrary[Html] = fixed(Html("<span />"))
  implicit val arbForm: Arbitrary[Form[_]] = fixed(booleanForm)
  implicit val arbAltAmendsActivity: Arbitrary[Form[AltAmendsActivity]] = fixed(models.RsFormMappings.altActivityForm()(messages))
  implicit val arbCheckFileType: Arbitrary[Form[CheckFileType]] = fixed(models.RsFormMappings.checkFileTypeForm()(messages))
  implicit val arbRS_groupScheme: Arbitrary[Form[RS_groupScheme]] = fixed(models.RsFormMappings.groupForm()(messages))
  implicit val arbCompanyName: Arbitrary[Form[Company]] = fixed(models.RsFormMappings.companyNameForm()(messages))
  implicit val arbCompanyBased: Arbitrary[Form[CompanyBasedInUk]] = fixed(models.RsFormMappings.companyBasedInUkForm()(messages))
  implicit val arbCompanyAddressUk: Arbitrary[Form[CompanyAddress]] = fixed(models.RsFormMappings.companyAddressUkForm()(messages))
  implicit val arbCompanyAddressOverseas: Arbitrary[Form[CompanyAddress]] = fixed(models.RsFormMappings.companyAddressOverseasForm()(messages))
  implicit val arbReportableEvents: Arbitrary[Form[ReportableEvents]] = fixed(models.RsFormMappings.chooseForm()(messages))
  implicit val arbTrusteeBasedInUk: Arbitrary[Form[TrusteeBasedInUk]] = fixed(models.RsFormMappings.trusteeBasedInUkForm())
  implicit val arbTrusteeName: Arbitrary[Form[TrusteeName]] = fixed(models.RsFormMappings.trusteeNameForm()(messages))
  implicit val arbCountryCodes: Arbitrary[CountryCodes] = fixed(app.injector.instanceOf[CountryCodesImpl])
  override implicit val arbAsciiString: Arbitrary[String] = fixed("/")

  val viewPackageName = "views.html"

  val layoutClasses: Seq[Class[_]] = Seq(classOf[govuk_wrapper])

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case alterations_activity: alterations_activity => render(alterations_activity)
    case alterations_amends: alterations_amends => render(alterations_amends)
    case check_csv_file: check_csv_file => render(check_csv_file)
    case check_file_type: check_file_type => render(check_file_type)
    case confirmation: confirmation =>
      val arbDateString: Arbitrary[String] = fixed("05 December 2023, 10:06AM")
      render(confirmation)(arbRequestObject, arbDateString, arbAsciiString, arbAsciiString, arbAsciiString, arbRequest, arbMessages, arbErsUtil, arbConfig)
    case file_upload_errors: file_upload_errors => render(file_upload_errors)
    case file_upload_problem: file_upload_problem => render(file_upload_problem)
    case global_error: global_error => render(global_error)
    case group: group => render(group)
    case group_plan_summary: group_plan_summary => render(group_plan_summary)
    case not_authorised: not_authorised => render(not_authorised)
    case page_not_found_template: page_not_found_template => render(page_not_found_template)
    case reportable_events: reportable_events => render(reportable_events)
    case signedOut: signedOut =>
      implicit val arbitraryRequest: Arbitrary[Request[AnyContent]] = fixed(fakeRequest)
      render(signedOut)
    case summary: summary =>
      implicit val arbAsciiString: Arbitrary[String] = fixed("1")
      render(summary)
    case trustee_address_overseas: trustee_address_overseas =>
      implicit val arbTrusteeAddressOverseas: Arbitrary[Form[TrusteeAddress]] = fixed(models.RsFormMappings.trusteeAddressOverseasForm()(messages))
      render(trustee_address_overseas)
    case trustee_address_uk: trustee_address_uk =>
      implicit val arbTrusteeAddressUK: Arbitrary[Form[TrusteeAddress]] = fixed(models.RsFormMappings.trusteeAddressUkForm()(messages))
      render(trustee_address_uk)
    case trustee_based_in_uk: trustee_based_in_uk => render(trustee_based_in_uk)
    case trustee_name: trustee_name => render(trustee_name)
    case trustee_remove_problem: trustee_remove_problem => render(trustee_remove_problem)
    case trustee_remove_yes_no: trustee_remove_yes_no => render(trustee_remove_yes_no)
    case trustee_summary: trustee_summary => render(trustee_summary)
    case unauthorised: unauthorised => render(unauthorised)
    case upscan_csv_file_upload: upscan_csv_file_upload => render(upscan_csv_file_upload)
    case upscan_ods_file_upload: upscan_ods_file_upload => render(upscan_ods_file_upload)
    case start: start => render(start)
  }

  runAccessibilityTests()
}
