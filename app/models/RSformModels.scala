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

import org.joda.time.DateTime
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Request
import utils.DateUtils

case class RS_scheme(scheme: String)

case class ReportableEvents(isNilReturn: Option[String])
object ReportableEvents {
  implicit val format: OFormat[ReportableEvents] = Json.format[ReportableEvents]
}

case class CheckFileType(checkFileType: Option[String])
object CheckFileType {
  implicit val format: OFormat[CheckFileType] = Json.format[CheckFileType]
}

case class RS_schemeType (schemeType: String)

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
                                      )
object SchemeOrganiserDetails {
  implicit val format: OFormat[SchemeOrganiserDetails] = Json.format[SchemeOrganiserDetails]
}

case class TrusteeDetails(
                              name: String,
                              addressLine1: String,
                              addressLine2: Option[String],
                              addressLine3: Option[String],
                              addressLine4: Option[String],
                              country: Option[String],
                              postcode: Option[String]
                              )
object TrusteeDetails {
  implicit val format: OFormat[TrusteeDetails] = Json.format[TrusteeDetails]
}


case class TrusteeDetailsList(trustees: List[TrusteeDetails])
object TrusteeDetailsList {
  implicit val format: OFormat[TrusteeDetailsList] = Json.format[TrusteeDetailsList]
}

case class CsvFiles(fileId: String, isSelected: Option[String])
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
      DateTime.now,
      getSchemeId,
      getTaxYear,
      getSchemeName,
      getSchemeType
    )

  def toErsMetaData(implicit request: Request[AnyRef]): ErsMetaData = {
    ErsMetaData(
      toSchemeInfo,
      request.remoteAddress,
      aoRef,
      getEmpRef,
      agentRef,
      None
    )
  }

  def getPageTitle(implicit messages: Messages): String = {
    s"${messages(s"ers.scheme.$getSchemeType")} " +
      s"- ${messages("ers.scheme.title", getSchemeNameForDisplay)} " +
      s"- $getSchemeReference - ${DateUtils.getFullTaxYear(getTaxYear)}"
  }


  def getTaxYear: String = taxYear.getOrElse("")

  def getSchemeReference: String = ersSchemeRef.getOrElse("")

  def getSchemeName: String = schemeName.getOrElse("")

  def getSchemeType: String = schemeType.getOrElse("")

  def getSchemeNameForDisplay(implicit messages: Messages): String =
    if(schemeName.isDefined) messages(s"ers.${getSchemeType.toLowerCase}") else ""

  def getEmpRef: String = empRef.getOrElse("")

  def getTS: String = ts.getOrElse("")

  def getHMAC: String = hmac.getOrElse("")

  def concatenateParameters: String = {
    getNVPair("agentRef", agentRef) +
      getNVPair("aoRef", aoRef) +
      getNVPair("empRef", empRef) +
      getNVPair("ersSchemeRef", ersSchemeRef) +
      getNVPair("schemeName", schemeName) +
      getNVPair("schemeType", schemeType) +
      getNVPair("taxYear", taxYear) +
      getNVPair("ts", ts)
  }

  def getSchemeId: String = {
    getSchemeType.toUpperCase match {
      case "CSOP" => "1"
      case "EMI" => "2"
      case "SAYE" => "4"
      case "SIP" => "5"
      case "OTHER" => "3"
      case _ => ""
    }
  }

  private def getNVPair(paramName: String, value: Option[String]): String = {
    value.map(paramName + "=" + _ + ";").getOrElse("")
  }
}
object RequestObject {
  implicit val formatRequestObject: OFormat[RequestObject] = Json.format[RequestObject]
}
