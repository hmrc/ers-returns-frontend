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

package utils

import config.ApplicationConfig
import models._
import play.api.Logging
import play.api.i18n.Messages

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ERSUtil @Inject() (val appConfig: ApplicationConfig)
                        (implicit val ec: ExecutionContext, countryCodes: CountryCodes) extends PageBuilder with Constants with Logging {

	final def concatAddress(optionalAddressLines: List[Option[String]], existingAddressLines: String): String = {
		val definedStrings = optionalAddressLines.filter(_.isDefined).map(_.get)
		existingAddressLines ++ definedStrings.map(addressLine => ", " + addressLine).mkString("")
	}

	def buildAddressSummary[A](entity: A): String = {
		entity match {
			case companyDetails: CompanyDetails =>
				val optionalAddressLines = List(
					companyDetails.addressLine2,
					companyDetails.addressLine3,
					companyDetails.addressLine4,
					companyDetails.addressLine5,
					countryCodes.getCountry(companyDetails.country.getOrElse(""))
				)
				concatAddress(optionalAddressLines, companyDetails.addressLine1)
			case trusteeDetails: TrusteeDetails =>
				val optionalAddressLines = List(trusteeDetails.addressLine2,
					trusteeDetails.addressLine3,
					trusteeDetails.addressLine4,
					trusteeDetails.addressLine5,
					countryCodes.getCountry(trusteeDetails.country.getOrElse(""))
				)
				concatAddress(optionalAddressLines, trusteeDetails.addressLine1)
			case _ => ""
		}
	}

  final def concatEntity(optionalLines: List[Option[String]], existingEntityLines: String): String = {
    val definedStrings = optionalLines.flatten
    existingEntityLines ++ definedStrings.map(addressLine => ", " + addressLine).mkString("")
  }

  def buildEntitySummary(entity: SchemeOrganiserDetails): String = {
    val optionalLines = List(
      entity.addressLine2,
      entity.addressLine3,
      entity.addressLine4,
      entity.country,
      entity.postcode,
      entity.companyReg,
      entity.corporationRef
    )
    concatEntity(optionalLines, s"${entity.companyName}, ${entity.addressLine1}")
  }

  def buildCompanyNameList(
    companyDetailsList: List[CompanyDetails],
    n: Int = 0,
    companyNamesList: String = ""
  ): String =
    if (n == companyDetailsList.length) { companyNamesList }
    else {
      buildCompanyNameList(companyDetailsList, n + 1, companyNamesList + companyDetailsList(n).companyName + "<br>")
    }

  def buildTrusteeNameList(
    trusteeDetailsList: List[TrusteeDetails],
    n: Int = 0,
    trusteeNamesList: String = ""
  ): String =
    if (n == trusteeDetailsList.length) { trusteeNamesList }
    else { buildTrusteeNameList(trusteeDetailsList, n + 1, trusteeNamesList + trusteeDetailsList(n).name + "<br>") }

	def companyLocation(company: CompanyDetails): String = if (company.basedInUk) "ers_trustee_based.uk" else "ers_trustee_based.overseas"


  def trusteeLocationMessage(trustee: TrusteeDetails): String = if (trustee.basedInUk) "ers_trustee_based.uk" else "ers_trustee_based.overseas"

  def addCompanyMessage(messages: Messages, schemeOpt: Option[String]): String =
    messages.apply(s"ers_group_summary.${schemeOpt.getOrElse("").toLowerCase}.add_company")

  def replaceAmpersand(input: String): String =
    appConfig.ampersandRegex
      .replaceAllIn(input, "&amp;")
}
