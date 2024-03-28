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

import java.time.{Instant, ZonedDateTime, ZoneId}
import play.api.libs.json._

import scala.util._

trait LocalDateTimeFormat {
  val validationError: JsonValidationError = JsonValidationError("Failed to find matching Json field Type")
  // Overriding the default play 2.6 LocalDateTime format to allow backwards compatibility with play 2.5 services (ers-submissions)
  implicit val dateFormatDefault: Format[ZonedDateTime] = new Format[ZonedDateTime] {
    override def reads(json: JsValue): JsResult[ZonedDateTime] = json match {
      case JsNumber(numberDate: BigDecimal) =>
        Try(ZonedDateTime.ofInstant(Instant.ofEpochMilli(numberDate.toLong), ZoneId.systemDefault())) match {
          case Success(date: ZonedDateTime) => JsSuccess(date)
          case _                        => JsError(JsonValidationError(s"[LocalDateTimeFormat][JsNumber]: Failed to parse date ($numberDate) to LocalDateTime"))
        }
      case _                            => JsError(validationError)
    }
    override def writes(o: ZonedDateTime): JsValue = JsNumber(o.toInstant.toEpochMilli)
  }
}

case class SchemeInfo(
  schemeRef: String,
  timestamp: ZonedDateTime = ZonedDateTime.now,
  schemeId: String,
  taxYear: String,
  schemeName: String,
  schemeType: String
)

object SchemeInfo extends LocalDateTimeFormat {
  implicit val format: OFormat[SchemeInfo]         = Json.format[SchemeInfo]
}

case class ErsMetaData(
  schemeInfo: SchemeInfo,
  ipRef: String,
  aoRef: Option[String],
  empRef: String,
  agentRef: Option[String],
  sapNumber: Option[String]
)

object ErsMetaData extends LocalDateTimeFormat {
  implicit val format: OFormat[ErsMetaData] = Json.format[ErsMetaData]
}

case class Person(name: String, relation: String)

case class AlterationAmends(
  altAmendsTerms: Option[String],
  altAmendsEligibility: Option[String],
  altAmendsExchange: Option[String],
  altAmendsVariations: Option[String],
  altAmendsOther: Option[String]
)

object AlterationAmends {
  implicit val format: OFormat[AlterationAmends] = Json.format[AlterationAmends]
}

case class CompanyDetails(
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
object CompanyDetails {
  implicit val format: OFormat[CompanyDetails] = Json.format[CompanyDetails]
}
case class CompanyDetailsList(companies: List[CompanyDetails])

object CompanyDetailsList {
  implicit val format: OFormat[CompanyDetailsList] = Json.format[CompanyDetailsList]
}
case class GroupSchemeInfo(
  groupScheme: Option[String],
  groupSchemeType: Option[String]
)
object GroupSchemeInfo {
  implicit val format: OFormat[GroupSchemeInfo] = Json.format[GroupSchemeInfo]
}

case class ErsSummary(
  bundleRef: String,
  isNilReturn: String,
  fileType: Option[String],
  confirmationDateTime: ZonedDateTime,
  metaData: ErsMetaData,
  altAmendsActivity: Option[AltAmendsActivity],
  alterationAmends: Option[AlterationAmends],
  groupService: Option[GroupSchemeInfo],
  schemeOrganiser: Option[SchemeOrganiserDetails],
  companies: Option[CompanyDetailsList],
  trustees: Option[TrusteeDetailsList],
  nofOfRows: Option[Int],
  transferStatus: Option[String]
)
object ErsSummary extends LocalDateTimeFormat {
  implicit val format: OFormat[ErsSummary] = Json.format[ErsSummary]
}
