/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.data.validation.Constraints
import play.api.data.validation.Constraints._
import play.api.i18n.Messages

object RsFormMappings {

  import fieldValidationPatterns._
  val postcodeMinLength: Int      = 6
  val postcodeMaxLength: Int      = 8
  /*
   * scheme type Form definition.
   */
  val schemeForm: Form[RS_scheme] = Form(mapping("scheme" -> text)(RS_scheme.apply)(RS_scheme.unapply))

  /*
   * activity type Form definition
   */
  def chooseForm()(implicit messages: Messages): Form[ReportableEvents] = Form(
    mapping(
      reportableEventsFields.isNilReturn -> optional(text)
        .verifying(Messages("ers_choose.err.message"), _.nonEmpty)
        .verifying(
          Messages("ers.invalidCharacters"),
          so => validInputCharacters(so.getOrElse("1"), fieldValidationPatterns.yesNoRegPattern)
        )
    )(ReportableEvents.apply)(ReportableEvents.unapply)
  )

  /*
   * check file type Form definition
   */
  def checkFileTypeForm()(implicit messages: Messages): Form[CheckFileType] = Form(mapping(
    checkFileTypeFields.checkFileType ->
      optional(text).verifying(Messages("ers_check_file_type.err.message"), _.nonEmpty)
        .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so.getOrElse("csv"), csvOdsRegPattern))
  )(CheckFileType.apply)(CheckFileType.unapply))

  /*
   * Is a group scheme Form definition
   */
  def groupForm()(implicit messages: Messages): Form[RS_groupScheme] = Form(
    mapping(
      "groupScheme" ->
        optional(text)
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so.getOrElse("1"), yesNoRegPattern))
    )(RS_groupScheme.apply)(RS_groupScheme.unapply)
  )

  /*
   * Is a group scheme type Form definition
   */
  def groupTypeForm(): Form[RS_groupSchemeType]                               =
    Form(mapping("groupSchemeType" -> text)(RS_groupSchemeType.apply)(RS_groupSchemeType.unapply))

  /*
   * Alterations Activity Form definition
   */
  def altActivityForm()(implicit messages: Messages): Form[AltAmendsActivity] =
    Form(
      mapping(
        "altActivity" -> text
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))
      )(AltAmendsActivity.apply)(AltAmendsActivity.unapply)
    )

  /*
   * Alterations Amends Form definition
   */
  def altAmendsForm()(implicit messages: Messages): Form[AltAmends] = Form(
    mapping(
      "altAmendsTerms"       -> optional(
        text
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))
      ),
      "altAmendsEligibility" -> optional(
        text
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))
      ),
      "altAmendsExchange"    -> optional(
        text
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))
      ),
      "altAmendsVariations"  -> optional(
        text
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))
      ),
      "altAmendsOther"       -> optional(
        text
          .verifying("required field", _.nonEmpty)
          .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, yesNoRegPattern))
      )
    )(AltAmends.apply)(AltAmends.unapply)
  )

  /*
   * CSV file check
   */

  def csvFileCheckForm()(implicit messages: Messages): Form[CsvFilesList] = Form(
    mapping(
      "files" -> list(
        mapping(
          "fileId" -> text
            .verifying("required field", _.nonEmpty)
            .verifying(Messages("ers.invalidCharacters"), so => validInputCharacters(so, csvFileNameRegx))
        )(CsvFiles.apply)(CsvFiles.unapply)
      )
    )(CsvFilesList.apply)(CsvFilesList.unapply)
  )

  def addTrusteeForm(): Form[AddTrustee] = Form(mapping(
    "addTrustee" -> nonEmptyText
      .transform(int => if (int == "0") true else false, (bool: Boolean) => if (bool) "0" else "1")
  )(AddTrustee.apply)(AddTrustee.unapply))

  def trusteeBasedInUkForm(): Form[TrusteeBasedInUk] = Form(mapping(
    trusteeBasedInUkFields.basedInUk -> nonEmptyText
      .transform(int => if (int == "0") true else false, (bool: Boolean) => if (bool) "0" else "1")
  )(TrusteeBasedInUk.apply)(TrusteeBasedInUk.unapply))

  def trusteeNameForm()(implicit messages: Messages): Form[TrusteeName] = Form(mapping(
    trusteeNameFields.name -> text
      .verifying(Messages("ers_trustee_details.err.summary.name_required"), _.nonEmpty)
      .verifying(Messages("ers_trustee_details.err.name"), so => checkLength(so, "trusteeNameFields.name"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.name"), so => validInputCharacters(so, addresssRegx))
  )(TrusteeName.apply)(TrusteeName.unapply))

  def trusteeAddressOverseasForm()(implicit messages: Messages): Form[TrusteeAddress] = Form(mapping(
    trusteeAddressFields.addressLine1 -> text
      .verifying(Messages("ers_trustee_details.err.summary.address_line1_required"), _.nonEmpty)
      .verifying(Messages("ers_trustee_details.err.address_line1"), so => checkAddressLength(so, "trusteeAddressFields.addressLine1"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    trusteeAddressFields.addressLine2 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line2"), so => checkAddressLength(so, "trusteeAddressFields.addressLine2"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.addressLine3 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line3"), so => checkAddressLength(so, "trusteeAddressFields.addressLine3"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.addressLine4 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line4"), so => checkAddressLength(so, "trusteeAddressFields.addressLine4"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.addressLine5 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line5"), so => checkAddressLength(so, "trusteeAddressFields.addressLine5"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line5"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.country -> optional(text
      verifying pattern(countryCodeRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country")))
  )(TrusteeAddress.apply)(TrusteeAddress.unapply))

  def trusteeAddressUkForm()(implicit messages: Messages): Form[TrusteeAddress] = Form(mapping(
    trusteeAddressFields.addressLine1 -> text
      .verifying(Messages("ers_trustee_details.err.summary.address_line1_required"), _.nonEmpty)
      .verifying(Messages("ers_trustee_details.err.address_line1"), so => checkAddressLength(so, "trusteeAddressFields.addressLine1"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    trusteeAddressFields.addressLine2 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line2"), so => checkAddressLength(so, "trusteeAddressFields.addressLine2"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.addressLine3 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line3"), so => checkAddressLength(so, "trusteeAddressFields.addressLine3"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.addressLine4 -> optional(text
      .verifying(Messages("ers_trustee_details.err.address_line4"), so => checkAddressLength(so, "trusteeAddressFields.addressLine4"))
      .verifying(Messages("ers_trustee_details.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    trusteeAddressFields.addressLine5 -> optional(text)
      .transform((x: Option[String]) => x.map(_.toUpperCase()), (z: Option[String]) => z.map(_.toUpperCase()))
      .verifying(Messages("ers_trustee_details.err.postcode"), so => isValidPostcode(so)),
    trusteeAddressFields.country -> optional(text
      .verifying(pattern(countryCodeRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country"))))
  )(TrusteeAddress.apply)(TrusteeAddress.unapply))

  def companyAddressUkForm()(implicit messages: Messages): Form[CompanyAddress] = Form(mapping(
    companyAddressFields.addressLine1 -> text.verifying(Messages("ers_manual_company_details.err.summary.address_line1_required"), _.nonEmpty)
      .verifying(Messages("ers_manual_company_details.err.address_line1"), so => checkAddressLength(so, "addressLine1"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    companyAddressFields.addressLine2 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line2"), so => checkAddressLength(so, "addressLine2"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.addressLine3 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line3"), so => checkAddressLength(so, "addressLine3"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.addressLine4 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line4"), so => checkAddressLength(so, "addressLine4"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.addressLine5 -> optional(text)
      .transform((x: Option[String]) => x.map(_.toUpperCase()), (z: Option[String]) => z.map(_.toUpperCase()))
      .verifying(Messages("ers_manual_company_details.err.postcode"), so => isValidPostcode(so)),
    companyAddressFields.country -> optional(text
      .verifying(pattern(countryCodeRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country"))))
  )(CompanyAddress.apply)(CompanyAddress.unapply))

  def companyAddressOverseasForm()(implicit messages: Messages): Form[CompanyAddress] = Form(mapping(
    companyAddressFields.addressLine1 -> text.verifying(Messages("ers_manual_company_details.err.summary.address_line1_required"), _.nonEmpty)
      .verifying(Messages("ers_manual_company_details.err.address_line1"), so => checkAddressLength(so, "addressLine1"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line1"), so => validInputCharacters(so, addresssRegx)),
    companyAddressFields.addressLine2 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line2"), so => checkAddressLength(so, "addressLine2"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line2"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.addressLine3 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line3"), so => checkAddressLength(so, "addressLine3"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line3"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.addressLine4 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line4"), so => checkAddressLength(so, "addressLine4"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line4"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.addressLine5 -> optional(text
      .verifying(Messages("ers_manual_company_details.err.address_line5"), so => checkAddressLength(so, "addressLine5"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.address_line5"), so => validInputCharacters(so, addresssRegx))),
    companyAddressFields.country -> optional(text
      .verifying(pattern(countryCodeRegx.r, error = Messages("ers_scheme_organiser.err.summary.invalid_country"))))
  )(CompanyAddress.apply)(CompanyAddress.unapply))

  /*
  Subsidiary Company Form Definition
   */

  def companyBasedInUkForm()(implicit messages: Messages): Form[CompanyBasedInUk] = Form(mapping(
    companyBasedInUkFields.basedInUk -> text
      .verifying(Constraints.nonEmpty(errorMessage = "problem with companyBasedInUkForm"))
      .verifying(Messages("ers_trustee_details.err.summary.name_required"), _.nonEmpty)
      .transform(int => if (int == "0") true else false, (bool: Boolean) => if (bool) "0" else "1")
  )(CompanyBasedInUk.apply)(CompanyBasedInUk.unapply))

  object companyBasedInUkFields {
    val basedInUk = "basedInUk"
  }

  def companyNameForm(isSchemeOrganiser: Boolean = false)(implicit messages: Messages): Form[Company] = {
    val prefix = if (isSchemeOrganiser) ("schemeOrganiserFields", "ers_scheme_organiser") else ("companyDetailsFields", "ers_manual_company_details")
    Form(mapping(
    companyNameFields.companyName -> text
      .verifying(Messages("ers_manual_company_details.err.summary.company_name_required"), _.nonEmpty)
      .verifying(Messages(s"${prefix._2}.err.company_name"), so => checkLength(so, s"${prefix._1}.companyName"))
      .verifying(Messages("ers_manual_company_details.err.invalidChars.company_name"), so => validInputCharacters(so, addresssRegx)),
    companyNameFields.companyReg -> optional(text
      .verifying(Messages("ers_manual_company_details.err.company_reg"), so => checkLength(so, "companyDetailsFields.companyReg"))
      .verifying(pattern(fieldValidationPatterns.companyRegPattern.r, error = Messages("ers_manual_company_details.err.company_reg")))),
    companyNameFields.corporationRef -> optional(text
      .verifying(pattern(corporationRefPattern.r, error = Messages("ers_manual_company_details.err.corporation_ref"))))
  )(Company.apply)(Company.unapply))
  }

  def addSubsidiaryForm(): Form[AddCompany] = Form(mapping(
    "addCompany" -> nonEmptyText
      .transform(int => if (int == "0") true else false, (bool: Boolean) => if (bool) "0" else "1")
  )(AddCompany.apply)(AddCompany.unapply))

  /*
* scheme type Form definition.
*/
  def schemeTypeForm(): Form[RS_schemeType] = Form(
    mapping("schemeType" -> text)(RS_schemeType.apply)(RS_schemeType.unapply)
  )

	def checkAddressLength(so: String, field: String): Boolean = {
		field.split('.').last match {
			case "addressLine1" | "addressLine2" | "addressLine3" => so.length <= 27
			case "addressLine4" | "addressLine5"                  => so.length <= 18
			case _                                                => false
		}
	}

  def checkLength(so: String, field: String): Boolean = field match {
    case "companyDetailsFields.companyName" | "trusteeNameFields.name" => so.length <= 120
    case "schemeOrganiserFields.companyName" => so.length <= 35
		case "companyDetailsFields.companyReg" => so.length == 8
    case _ => false
  }

  def validInputCharacters(field: String, regXValue: String): Boolean = field.matches(regXValue)

  def isValidPostcode(input: Option[String]): Boolean = input match {
    case Some(postcode) => postcode.toUpperCase.matches(postCodeRegx) && isValidLengthIfPopulated(postcode, postcodeMinLength, postcodeMaxLength)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidPostcodeSchemeOrganiser(input: Option[String]): Boolean = input match {
    case Some(postcode) => postcode.toUpperCase.replaceAll(" ","").matches(fieldValidationPatterns.onlyCharsAndDigitsRegex)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidLengthPostcode(input: Option[String]): Boolean = input match {
    case Some(postcode) => isValidLengthIfPopulated(postcode, postcodeMinLength, postcodeMaxLength)
    case None           => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidFormatPostcodeSchemeOrganiser(input: Option[String]): Boolean = input match {
    case Some(postcode) => postcode.toUpperCase.matches(fieldValidationPatterns.postCodeRegx)
    case None => true //Postcode is assumed to be optional so return true if missing
  }

  def isValidLengthIfPopulated(input: String, minSize: Int, maxSize: Int): Boolean =
    input.trim.length >= minSize && input.trim.length <= maxSize

}

object reportableEventsFields {
  val isNilReturn = "isNilReturn"
}

object checkFileTypeFields {
  val checkFileType = "checkFileType"
}

object trusteeBasedInUkFields {
  val basedInUk = "basedInUk"
}

object trusteeNameFields {
  val name = "name"
}

object trusteeAddressFields {
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val addressLine5 = "addressLine5"
  val country = "country"
}

object companyDetailsFields {
  val companyName    = "companyName"
  val addressLine1   = "addressLine1"
  val addressLine2   = "addressLine2"
  val addressLine3   = "addressLine3"
  val addressLine4   = "addressLine4"
  val country        = "country"
  val postcode       = "postcode"
  val companyReg     = "companyReg"
  val corporationRef = "corporationRef"

}

object companyAddressFields {
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val addressLine4 = "addressLine4"
  val addressLine5 = "addressLine5"
  val country = "country"
}

object companyNameFields {
  val companyName = "companyName"
  val companyReg = "companyReg"
  val corporationRef = "corporationRef"
}

object fieldValidationPatterns {

  val companyRegPattern = "(?i)^([0-9]\\d{6,7}|\\d{6,7}|[A-Z]{2}\\d{6})$"

  def onlyCharsAndDigitsRegex = "^[a-zA-Z0-9]*$"

  def corporationRefPattern = "^([0-9]{10})$"

  def corporationRefPatternSchemeOrg = "^[0-9]*$"

  def addresssRegx = """^[A-Za-zÂ-ȳ0-9 &'(),-./]{0,}$"""
  
  val countryCodeRegx = "^[A-Z]{2}$"

  val postCodeRegx =
    """(GIR 0AA)|((([A-Z-[QVX]][0-9][0-9]?)|(([A-Z-[QVX]][A-Z-[IJZ]][0-9][0-9]?)|(([A-Z-[QVX‌​]][0-9][A-HJKSTUW])|([A-Z-[QVX]][A-Z-[IJZ]][0-9][ABEHMNPRVWXY]))))\s?[0-9][A-Z-[C‌​IKMOV]]{2})"""

  val yesNoRegPattern = "^([1-2]{1})$"

  val csvOdsRegPattern = "^((ods|csv))$"

  val csvFileNameRegx = """^file[0-9]{1}$"""
}
