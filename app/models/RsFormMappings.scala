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

package models

import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

object RsFormMappings {

	import fieldValidationPatterns._

  val postcodeMinLength: Int = 6
  val postcodeMaxLength: Int = 8
  /*
  * scheme type Form definition.
  */
  val schemeForm: Form[RS_scheme] = Form(mapping("scheme" -> text)(RS_scheme.apply)(RS_scheme.unapply))


  /*
   * activity type Form definition
   */
  def chooseForm()(implicit messages: Messages): Form[ReportableEvents] = Form(mapping(reportableEventsFields.isNilReturn -> optional(text)
    .verifying(Messages("ers_choose.err.message"), _.nonEmpty)
    .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so.getOrElse("1"),
      fieldValidationPatterns.yesNoRegPattern)))(ReportableEvents.apply)(ReportableEvents.unapply))

  /*
   * check file type Form definition
   */
  def checkFileTypeForm()(implicit messages: Messages): Form[CheckFileType] = Form(mapping(
    checkFileTypeFileds.checkFileType ->
      optional(text).verifying(Messages("ers_check_file_type.err.message"), _.nonEmpty)
        .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so.getOrElse("csv"), csvOdsRegPattern))
  )(CheckFileType.apply)(CheckFileType.unapply))

  /*
   * Is a group scheme Form definition
   */
  def groupForm()(implicit messages: Messages): Form[RS_groupScheme] = Form(mapping("groupScheme" ->
    optional(text).verifying("required field", _.nonEmpty)
      .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so.getOrElse("1"), yesNoRegPattern))
  )(RS_groupScheme.apply)(RS_groupScheme.unapply))

  /*
  * Is a group scheme type Form definition
  */
  def groupTypeForm()(implicit messages: Messages): Form[RS_groupSchemeType] = {
		Form(mapping("groupSchemeType" -> text)(RS_groupSchemeType.apply)(RS_groupSchemeType.unapply))
	}

	/*
   * Alterations Activity Form definition
   */
  def altActivityForm()(implicit messages: Messages): Form[AltAmendsActivity] = {
		Form(mapping("altActivity" -> text
			.verifying("required field", _.nonEmpty)
			.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern)))
		(AltAmendsActivity.apply)(AltAmendsActivity.unapply))
	}

  /*
   * Alterations Amends Form definition
   */
  def altAmendsForm()(implicit messages: Messages): Form[AltAmends] = Form(mapping(
    "altAmendsTerms" -> optional(text.verifying("required field", _.nonEmpty)
			.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))),
    "altAmendsEligibility" -> optional(text.verifying("required field", _.nonEmpty)
			.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))),
    "altAmendsExchange" -> optional(text.verifying("required field", _.nonEmpty)
			.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))),
    "altAmendsVariations" -> optional(text.verifying("required field", _.nonEmpty)
			.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))),
    "altAmendsOther" -> optional(text.verifying("required field", _.nonEmpty)
			.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))))(AltAmends.apply)(AltAmends.unapply))

  /*
  * CSV file check
  */

  def csvFileCheckForm()(implicit messages: Messages): Form[CsvFilesList] = Form(mapping(
    "files" -> list(
      mapping(
        "fileId" -> text.verifying("required field", _.nonEmpty)
					.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, csvFileNameRegx)),
        "isSelected" -> optional(text.verifying("required field", _.nonEmpty)
					.verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern)))
      )(CsvFiles.apply)(CsvFiles.unapply)
    )
  )(CsvFilesList.apply)(CsvFilesList.unapply))

  /*
   * Trustee Details Form definition
   */
  def trusteeDetailsForm()(implicit messages: Messages): Form[TrusteeDetails] = Form(mapping(
    trusteeDetailsFields.name -> text
			.verifying(Messages("ers_trustee_details.err.summary.name_required"), _.nonEmpty)
			.verifying(Messages("ers_trustee_details.err.name"), so => checkLength(so, "trusteeDetailsFields.name"))
			.verifying(Messages("ers_trustee_details.err.invalidChars.name"), so => validInputCharacters(so, addresssRegx)),
    trusteeDetailsFields.addressLine1 -> text.verifying(Messages("ers_trustee_details.err.summary.address_line1_required"), _.nonEmpty)
			.verifying(Messages("ers_trustee_details.err.address_line1"), so => checkAddressLength(so, "trusteeDetailsFields.addressLine1"))
			.verifying(Messages("ers_trustee_details.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    trusteeDetailsFields.addressLine2 -> optional(text
			.verifying(Messages("ers_trustee_details.err.address_line2"), so => checkAddressLength(so, "trusteeDetailsFields.addressLine2"))
			.verifying(Messages("ers_trustee_details.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    trusteeDetailsFields.addressLine3 -> optional(text
			.verifying(Messages("ers_trustee_details.err.address_line3"), so => checkAddressLength(so, "trusteeDetailsFields.addressLine3"))
			.verifying(Messages("ers_trustee_details.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    trusteeDetailsFields.addressLine4 -> optional(text
			.verifying(Messages("ers_trustee_details.err.address_line4"), so => checkAddressLength(so, "trusteeDetailsFields.addressLine4"))
			.verifying(Messages("ers_trustee_details.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    trusteeDetailsFields.country -> optional(text
			verifying pattern(addresssRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country"))),
    trusteeDetailsFields.postcode -> optional(text)
      .transform((x: Option[String]) => x.map(_.toUpperCase()), (z: Option[String]) => z.map(_.toUpperCase()))
      .verifying(Messages("ers_trustee_details.err.postcode"), so => isValidPostcode(so))
  )(TrusteeDetails.apply)(TrusteeDetails.unapply))

  /*
   * Manual Company Details Form definition
   */
  def companyDetailsForm()(implicit messages: Messages): Form[CompanyDetails] = Form(mapping(
    companyDetailsFields.companyName -> text
			.verifying(Messages("ers_manual_company_details.err.summary.company_name_required"), _.nonEmpty)
			.verifying(Messages("ers_manual_company_details.err.company_name"), so => checkLength(so, "companyDetailsFields.companyName"))
			.verifying(Messages("ers_manual_company_details.err.invalidChars.company_name"), so => validInputCharacters(so, addresssRegx)),
    companyDetailsFields.addressLine1 -> text
			.verifying(Messages("ers_manual_company_details.err.summary.address_line1_required"), _.nonEmpty)
			.verifying(Messages("ers_manual_company_details.err.address_line1"), so => checkAddressLength(so, "companyDetailsFields.addressLine1"))
			.verifying(Messages("ers_manual_company_details.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    companyDetailsFields.addressLine2 -> optional(text
			.verifying(Messages("ers_manual_company_details.err.address_line2"), so => checkAddressLength(so, "companyDetailsFields.addressLine2"))
			.verifying(Messages("ers_manual_company_details.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    companyDetailsFields.addressLine3 -> optional(text
			.verifying(Messages("ers_manual_company_details.err.address_line3"), so => checkAddressLength(so, "companyDetailsFields.addressLine3"))
			.verifying(Messages("ers_manual_company_details.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    companyDetailsFields.addressLine4 -> optional(text
			.verifying(Messages("ers_manual_company_details.err.address_line4"), so => checkAddressLength(so, "companyDetailsFields.addressLine4"))
			.verifying(Messages("ers_manual_company_details.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    companyDetailsFields.country -> optional(text
			verifying pattern(addresssRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country"))),
    companyDetailsFields.postcode -> optional(text)
      .transform((x: Option[String]) => x.map(_.toUpperCase()), (z: Option[String]) => z.map(_.toUpperCase()))

			.verifying(Messages("ers_manual_company_details.err.postcode"), so => isValidPostcode(so)),
    companyDetailsFields.companyReg -> optional(text
			verifying pattern(companyRegPattern.r, error = Messages("ers_manual_company_details.err.summary.company_reg_pattern"))),
    companyDetailsFields.corporationRef -> optional(text
			verifying pattern(corporationRefPattern.r, error = Messages("ers_manual_company_details.err.summary.corporation_ref_pattern")))
  )(CompanyDetails.apply)(CompanyDetails.unapply))

  /*
   * Scheme Organiser Form definition
   */
  def schemeOrganiserForm()(implicit messages: Messages): Form[SchemeOrganiserDetails] = Form(mapping(
    schemeOrganiserFields.companyName -> text
			.verifying(Messages("ers_scheme_organiser.err.summary.company_name_required"), _.nonEmpty)
			.verifying(Messages("ers_scheme_organiser.err.company_name"), so => checkLength(so, "schemeOrganiserFields.companyName"))
			.verifying(Messages("ers_scheme_organiser.err.invalidChars.company_name"), so => validInputCharacters(so, addresssRegx)),
    schemeOrganiserFields.addressLine1 -> text
			.verifying(Messages("ers_scheme_organiser.err.summary.address_line1_required"), _.nonEmpty)
			.verifying(Messages("ers_scheme_organiser.err.address_line1"), so => checkAddressLength(so, "schemeOrganiserFields.addressLine1"))
			.verifying(Messages("ers_scheme_organiser.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    schemeOrganiserFields.addressLine2 -> optional(text
			.verifying(Messages("ers_scheme_organiser.err.address_line2"), so => checkAddressLength(so, "schemeOrganiserFields.addressLine2"))
			.verifying(Messages("ers_scheme_organiser.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    schemeOrganiserFields.addressLine3 -> optional(text
			.verifying(Messages("ers_scheme_organiser.err.address_line3"), so => checkAddressLength(so, "schemeOrganiserFields.addressLine3"))
			.verifying(Messages("ers_scheme_organiser.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    schemeOrganiserFields.addressLine4 -> optional(text
			.verifying(Messages("ers_scheme_organiser.err.address_line4"), so => checkAddressLength(so, "schemeOrganiserFields.addressLine4"))
			.verifying(Messages("ers_scheme_organiser.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    schemeOrganiserFields.country -> optional(text
			verifying pattern(addresssRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country"))),
    schemeOrganiserFields.postcode -> optional(text)
      .transform((x: Option[String]) => x.map(_.toUpperCase()), (z: Option[String]) => z.map(_.toUpperCase()))
      .verifying(Messages("ers_scheme_organiser.err.postcode"), so => isValidLengthPostcode(so))
      .verifying(Messages("ers_scheme_organiser.err.invalidChars.postcode"), so => isValidPostcodeSchemeOrganiser(so))
      .verifying(Messages("ers_scheme_organiser.err.invalidFormat.postcode"), so => isValidFormatPostcodeSchemeOrganiser(so)),
    schemeOrganiserFields.companyReg -> optional(text
			.verifying(Messages("ers_scheme_organiser.err.summary.company_reg"), so => checkLength(so, "schemeOrganiserFields.companyRegistrationNum"))
			verifying pattern(fieldValidationPatterns.onlyCharsAndDigitsRegex.r, error = Messages("ers_scheme_organiser.err.summary.invalidChars.company_reg_pattern"))),
    schemeOrganiserFields.corporationRef -> optional(text verifying(Messages("ers_scheme_organiser.err.summary.corporation_ref"), so => checkLength(so, "schemeOrganiserFields.corporationTaxReference")) verifying pattern(fieldValidationPatterns.corporationRefPatternSchemeOrg.r, error = Messages("ers_scheme_organiser.err.summary.invalidChars.corporation_ref_pattern")))
  )(SchemeOrganiserDetails.apply)(SchemeOrganiserDetails.unapply))

  /*
* scheme type Form definition.
*/
  def schemeTypeForm()(implicit messages: Messages): Form[RS_schemeType] = Form(

    mapping("schemeType" -> text)(RS_schemeType.apply)(RS_schemeType.unapply))



	def checkAddressLength(so: String, field: String): Boolean = {
		field.split('.').last match {
			case "addressLine1" | "addressLine2" | "addressLine3" => so.length <= 27
			case "addressLine4" => so.length <= 18
			case _ => false
		}
	}

  def checkLength(so: String, field: String): Boolean = field match {
    case "companyDetailsFields.companyName" | "trusteeDetailsFields.name" => so.length <= 120
    case "schemeOrganiserFields.companyName" => so.length <= 35
		case "schemeOrganiserFields.companyRegistrationNum" => so.length <= 8
		case "schemeOrganiserFields.corporationTaxReference" => so.length == 10
    case _ => false
  }

  def validInputCharacters(field: String, regXValue: String): Boolean = field.matches(regXValue)

  def isValidPostcode(input: Option[String]): Boolean = input match {
    case Some(postcode) => postcode.matches(postCodeRegx) && isValidLengthIfPopulated(postcode, postcodeMinLength, postcodeMaxLength)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidPostcodeSchemeOrganiser(input: Option[String]): Boolean = input match {
    case Some(postcode) => postcode.replaceAll(" ","").matches(fieldValidationPatterns.onlyCharsAndDigitsRegex)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidLengthPostcode(input: Option[String]): Boolean = input match {
    case Some(postcode) => isValidLengthIfPopulated(postcode, postcodeMinLength, postcodeMaxLength)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidFormatPostcodeSchemeOrganiser(input: Option[String]): Boolean = input match {
    case Some(postcode) => postcode.matches(fieldValidationPatterns.postCodeRegx)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidLengthIfPopulated(input: String, minSize: Int, maxSize: Int): Boolean = input.trim.length >= minSize && input.trim.length <= maxSize

}

object reportableEventsFields {
  val isNilReturn = "isNilReturn"
}

object checkFileTypeFileds {
  val checkFileType = "checkFileType"
}

object trusteeDetailsFields {
  val name = "name"
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val country = "country"
  val postcode = "postcode"
}

object companyDetailsFields {
  val companyName = "companyName"
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val country = "country"
  val postcode = "postcode"
  val companyReg = "companyReg"
  val corporationRef = "corporationRef"
}

object schemeOrganiserFields {
  val companyName = "companyName"
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val country = "country"
  val postcode = "postcode"
  val companyReg = "companyReg"
  val corporationRef = "corporationRef"
}

object fieldValidationPatterns {

  val companyRegPattern = "(^[a-zA-Z0-9]{1,10})$"

  def onlyCharsAndDigitsRegex = "^[a-zA-Z0-9]*$"

  def corporationRefPattern = "^([0-9]{10})$"

  def corporationRefPatternSchemeOrg = "^[0-9]*$"

  def addresssRegx = """^[A-Za-zÂ-ȳ0-9 &'(),-./]{0,}$"""

  val postCodeRegx = """(GIR 0AA)|((([A-Z-[QVX]][0-9][0-9]?)|(([A-Z-[QVX]][A-Z-[IJZ]][0-9][0-9]?)|(([A-Z-[QVX‌​]][0-9][A-HJKSTUW])|([A-Z-[QVX]][A-Z-[IJZ]][0-9][ABEHMNPRVWXY]))))\s?[0-9][A-Z-[C‌​IKMOV]]{2})"""

  val yesNoRegPattern = "^([1-2]{1})$"

  val csvOdsRegPattern = "^((ods|csv))$"

  val csvFileNameRegx = """^file[0-9]{1}$"""
}
