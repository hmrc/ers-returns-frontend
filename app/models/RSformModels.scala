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

package models

import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.RequestHeader
import utils.{CountryCodes, DateUtils}

import java.time.Instant

case class RS_scheme(scheme: String)

case class ReportableEvents(isNilReturn: Option[String])
object ReportableEvents {
  implicit val format: OFormat[ReportableEvents] = Json.format[ReportableEvents]
}

case class CheckFileType(checkFileType: Option[String])
object CheckFileType {
  implicit val format: OFormat[CheckFileType] = Json.format[CheckFileType]
}

case class RS_schemeType(schemeType: String)

case class RS_groupSchemeType(groupSchemeType: String)

case class RS_groupScheme(groupScheme: Option[String])
object RS_groupScheme {
  implicit val format: OFormat[RS_groupScheme] = Json.format[RS_groupScheme]
}

case class AltAmendsActivity(altActivity: String)
object AltAmendsActivity {
  implicit val format: OFormat[AltAmendsActivity] = Json.format[AltAmendsActivity]
}

case class AltAmends(
  altAmendsTerms: Option[String],
  altAmendsEligibility: Option[String],
  altAmendsExchange: Option[String],
  altAmendsVariations: Option[String],
  altAmendsOther: Option[String]
)
object AltAmends {
  implicit val format: OFormat[AltAmends] = Json.format[AltAmends]
}

case class SchemeOrganiserDetails(
  companyName: String,
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  country: Option[String],
  postcode: Option[String],
  companyReg: Option[String],
  corporationRef: Option[String]
) {
  def toArray(countryCodes: CountryCodes): Array[String] =
    Array(
      companyName,
      addressLine1,
      addressLine2.getOrElse(""),
      addressLine3.getOrElse(""),
      addressLine4.getOrElse(""),
      countryCodes.getCountry(country.getOrElse("")).getOrElse(""),
      postcode.getOrElse(""),
      companyReg.getOrElse(""),
      corporationRef.getOrElse("")
    ).filter(_.nonEmpty)
}

object SchemeOrganiserDetails {
  implicit val format: OFormat[SchemeOrganiserDetails] = Json.format[SchemeOrganiserDetails]

  val emptyForm = SchemeOrganiserDetails(
    "",
    "",
    Some(""),
    Some(""),
    Some(""),
    Some("UK"),
    Some(""),
    Some(""),
    Some("")
  )
}

case class TrusteeDetails(
                              name: String,
                              addressLine1: String,
                              addressLine2: Option[String],
                              addressLine3: Option[String],
                              addressLine4: Option[String],
                              country: Option[String],
                              addressLine5: Option[String], // Postcode for UK address
                              basedInUk: Boolean
                              ) {

  def replaceName(trusteeName: TrusteeName): TrusteeDetails = {
    this.copy(name = trusteeName.name)
  }

  def replaceBasedInUk(trusteeBasedInUk: TrusteeBasedInUk): TrusteeDetails = {
    this.copy(basedInUk = trusteeBasedInUk.basedInUk)
  }

  def replaceAddress(trusteeAddress: TrusteeAddress): TrusteeDetails = {
    this.copy(
      addressLine1 = trusteeAddress.addressLine1,
      addressLine2 = trusteeAddress.addressLine2,
      addressLine3 = trusteeAddress.addressLine3,
      addressLine4 = trusteeAddress.addressLine4,
      country      = trusteeAddress.country,
      addressLine5 = trusteeAddress.addressLine5
    )
  }

  def updatePart[A](part: A): TrusteeDetails = {
    part match {
      case trusteeName: TrusteeName => replaceName(trusteeName)
      case trusteeBasedInUk: TrusteeBasedInUk => replaceBasedInUk(trusteeBasedInUk)
      case trusteeAddress: TrusteeAddress => replaceAddress(trusteeAddress)
      case _ => this
    }
  }
}

object TrusteeDetails {

  def apply(name: TrusteeName, address: TrusteeAddress): TrusteeDetails = {
    TrusteeDetails(
      name.name,
      address.addressLine1,
      address.addressLine2,
      address.addressLine3,
      address.addressLine4,
      address.country,
      address.addressLine5,
      address.country.fold(false)(_.equals("UK"))
    )
  }

  implicit val format: OFormat[TrusteeDetails] = Json.format[TrusteeDetails]
}

case class TrusteeBasedInUk(basedInUk: Boolean)

object TrusteeBasedInUk {
  implicit val format: OFormat[TrusteeBasedInUk] = Json.format[TrusteeBasedInUk]
}

case class TrusteeName(name: String)

object TrusteeName {
  implicit val format: OFormat[TrusteeName] = Json.format[TrusteeName]
}

case class TrusteeAddress(
                                   addressLine1: String,
                                   addressLine2: Option[String],
                                   addressLine3: Option[String],
                                   addressLine4: Option[String],
                                   addressLine5: Option[String],
                                   country: Option[String]
                                 )

object TrusteeAddress {
  implicit val format: OFormat[TrusteeAddress] = Json.format[TrusteeAddress]
}

case class TrusteeDetailsList(trustees: List[TrusteeDetails])
object TrusteeDetailsList {
  implicit val format: OFormat[TrusteeDetailsList] = Json.format[TrusteeDetailsList]
}

case class CsvFiles(fileId: String)
object CsvFiles {
  implicit val format: OFormat[CsvFiles] = Json.format[CsvFiles]
}

case class CsvFilesList(files: List[CsvFiles])
object CsvFilesList {
  implicit val format: OFormat[CsvFilesList] = Json.format[CsvFilesList]
}

case class RequestObject(
  aoRef: Option[String],
  taxYear: Option[String],
  ersSchemeRef: Option[String],
  schemeName: Option[String],
  schemeType: Option[String],
  agentRef: Option[String],
  empRef: Option[String],
  ts: Option[String],
  hmac: Option[String]
) {

  private def toSchemeInfo: SchemeInfo =
    SchemeInfo(
      getSchemeReference,
      Instant.now,
      getSchemeId,
      getTaxYear,
      getSchemeName,
      getSchemeType
    )

  def toErsMetaData(implicit request: RequestHeader): ErsMetaData =
    ErsMetaData(
      toSchemeInfo,
      request.remoteAddress,
      aoRef,
      getEmpRef,
      agentRef,
      None
    )

  def getPageTitle(implicit messages: Messages): String =
    s"${messages(s"ers.scheme.${getSchemeType.toUpperCase}")} " +
      s"- ${messages("ers.scheme.title", getSchemeNameForDisplay)} " +
      s"- $getSchemeReference - ${DateUtils.getFullTaxYear(getTaxYear)}"

  def getTaxYear: String = taxYear.getOrElse("")

  def getSchemeReference: String = ersSchemeRef.getOrElse("")

  def getSchemeName: String = schemeName.getOrElse("")

  def getSchemeType: String = schemeType.getOrElse("")

  def getSchemeNameForDisplay(implicit messages: Messages): String =
    if (schemeName.isDefined) messages(s"ers.${getSchemeType.toLowerCase}") else ""

  def getSchemeNameCaptionForDisplay(implicit messages: Messages): String =
    if (schemeName.isDefined) messages(s"ers.${getSchemeType.toLowerCase}.caption") else ""

  def getSchemeHeadingName(implicit messages: Messages): String =
    schemeType.map(schemeName => messages(s"ers.schemeDisplay.${schemeName.toUpperCase}")).getOrElse("")

  def getEmpRef: String = empRef.getOrElse("")

  def getTS: String = ts.getOrElse("")

  def getHMAC: String = hmac.getOrElse("")

  def concatenateParameters: String =
    getNVPair("agentRef", agentRef) +
      getNVPair("aoRef", aoRef) +
      getNVPair("empRef", empRef) +
      getNVPair("ersSchemeRef", ersSchemeRef) +
      getNVPair("schemeName", schemeName) +
      getNVPair("schemeType", schemeType) +
      getNVPair("taxYear", taxYear) +
      getNVPair("ts", ts)

  def getSchemeId: String =
    getSchemeType.toUpperCase match {
      case "CSOP"  => "1"
      case "EMI"   => "2"
      case "SAYE"  => "4"
      case "SIP"   => "5"
      case "OTHER" => "3"
      case _       => ""
    }

  private def getNVPair(paramName: String, value: Option[String]): String =
    value.map(paramName + "=" + _ + ";").getOrElse("")
}

object RequestObject {
  implicit val formatRequestObject: OFormat[RequestObject] = Json.format[RequestObject]

  def getSchemeWithArticle(schemeType: String)(implicit messages: Messages): String = {
    messages.lang.code match {
      case "en" =>
        val article = if (startsWithVowel(schemeType)) "an" else "a"
        s"$article $schemeType"

      case _ => // cy
        if (schemeType == "OTHER") messages(s"ers.scheme.$schemeType") else schemeType
    }
  }

  def getSchemeTypeForOdsSchemeMismatch(schemeType: String)(implicit messages: Messages): String = {
    s"${messages(s"ers.scheme.${schemeType.toUpperCase}")}"
  }

  def startsWithVowel(scheme: String): Boolean = {
    val trimmed = scheme.trim.toUpperCase
    trimmed.nonEmpty && "AEIOU".contains(trimmed.charAt(0))
  }

}

case class AddTrustee(addTrustee: Boolean)

case class AddCompany(addCompany: Boolean)
