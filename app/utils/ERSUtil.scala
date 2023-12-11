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

package utils

import config.{ApplicationConfig, ERSShortLivedCache}
import metrics.Metrics
import models._
import play.api.Logging
import play.api.i18n.Messages
import services.ERSFileValidatorSessionCacheServices
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ERSUtil @Inject()(
												 val sessionService: ERSFileValidatorSessionCacheServices,
												 val shortLivedCache: ERSShortLivedCache,
												 val appConfig: ApplicationConfig
											 )(implicit val ec: ExecutionContext, countryCodes: CountryCodes)
	extends PageBuilder
		with JsonParser
		with Metrics
		with HMACUtil
		with Logging {

	val largeFileStatus = "largefiles"
	val savedStatus = "saved"
	val ersMetaData: String = "ErsMetaData"
	val ersRequestObject: String = "ErsRequestObject"
	val reportableEvents = "ReportableEvents"
	val GROUP_SCHEME_CACHE_CONTROLLER: String = "group-scheme-controller"
	val ALT_AMENDS_CACHE_CONTROLLER: String = "alt-amends-cache-controller"
	val GROUP_SCHEME_COMPANIES: String = "group-scheme-companies"
	val csvFilesCallbackList: String = "csv-file-callback-List"

	// Cache Ids
	val SCHEME_CACHE: String = "scheme-type"
	val FILE_TYPE_CACHE: String = "check-file-type"
	val ERROR_COUNT_CACHE: String = "error-count"
	val ERROR_LIST_CACHE: String = "error-list"
	val ERROR_SUMMARY_CACHE: String = "error-summary"
	val CHOOSE_ACTIVITY_CACHE: String = "choose-activity"
	val GROUP_SCHEME_CACHE: String = "group-scheme"
	val GROUP_SCHEME_TYPE_CACHE: String = "group-scheme-type"
	val altAmendsActivity: String = "alt-activity"

	val CHECK_CSV_FILES: String = "check-csv-files"
	val CSV_FILES_UPLOAD: String = "csv-files-upload"

	val FILE_NAME_CACHE: String = "file-name"

	val SCHEME_ORGANISER_CACHE: String = "scheme-organiser"
	val TRUSTEES_CACHE: String = "trustees"
	val TRUSTEE_NAME_CACHE: String = "trustee-name"
	val TRUSTEE_BASED_CACHE: String = "trustee-based"
	val TRUSTEE_ADDRESS_CACHE: String = "trustee-address"
	val TRUSTEE_ADDRESS_UK_CACHE: String = "trustee-address-uk"
	val TRUSTEE_ADDRESS_OVERSEAS_CACHE: String = "trustee-address-overseas"
	val ERROR_REPORT_DATETIME: String = "error-report-datetime"

	// Params
	val PORTAL_AOREF_CACHE: String = "portal-ao-ref"
	val PORTAL_TAX_YEAR_CACHE: String = "portal-tax-year"
	val PORTAL_ERS_SCHEME_REF_CACHE: String = "portal-ers-scheme-ref"
	val PORTAL_SCHEME_TYPE: String = "portal-scheme-type"
	val PORTAL_SCHEME_NAME_CACHE: String = "portal-scheme-name"
	val PORTAL_HMAC_CACHE: String = "portal-hmac"
	val PORTAL_SCHEME_REF: String = "portal-scheme-ref"

	val CONFIRMATION_DATETIME_CACHE: String = "confirmation-date-time"

	// new cache amends
	val PORTAL_PARAMS_CACHE: String = "portal_params"

	val BUNDLE_REF: String = "sap-bundle-ref"
	val FILE_TRANSFER_CACHE = "file-tansfer-cache"
	val FILE_TRANSFER_CACHE_LIST = "file-transfer-cache-list"
	val IP_REF: String = "ip-ref"

	val VALIDATED_SHEEETS: String = "validated-sheets"

	def getStatus(tRows: Option[Int]): Some[String] =
		if (tRows.isDefined && tRows.get > appConfig.sentViaSchedulerNoOfRowsLimit) {
			Some(largeFileStatus)
		} else {
			Some(savedStatus)
		}

	def getNoOfRows(nilReturn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Int]] =
		if (isNilReturn(nilReturn: String)) {
			Future.successful(None)
		} else {
			sessionService.getSuccessfulCallbackRecord.map(res => res.flatMap(_.noOfRows))
		}

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
					companyDetails.postcode,
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
		if (n == companyDetailsList.length) {
			companyNamesList
		}
		else {
			buildCompanyNameList(companyDetailsList, n + 1, companyNamesList + companyDetailsList(n).companyName + "<br>")
		}

	def buildTrusteeNameList(
														trusteeDetailsList: List[TrusteeDetails],
														n: Int = 0,
														trusteeNamesList: String = ""
													): String =
		if (n == trusteeDetailsList.length) {
			trusteeNamesList
		}
		else {
			buildTrusteeNameList(trusteeDetailsList, n + 1, trusteeNamesList + trusteeDetailsList(n).name + "<br>")
		}

	private def getCacheId(implicit hc: HeaderCarrier): String =
		hc.sessionId.getOrElse(throw new RuntimeException("")).value

	def isNilReturn(nilReturn: String): Boolean = nilReturn == OPTION_NIL_RETURN

	def companyLocation(company: CompanyDetails): String =
		company.country
			.collect {
				case c if c != DEFAULT_COUNTRY => OVERSEAS
				case c => c
			}
			.getOrElse(DEFAULT)

	def trusteeLocationMessage(trustee: TrusteeDetails): String = if (trustee.basedInUk) "ers_trustee_based.uk" else "ers_trustee_based.overseas"

	def addCompanyMessage(messages: Messages, schemeOpt: Option[String]): String =
		messages.apply(s"ers_group_summary.${schemeOpt.getOrElse("").toLowerCase}.add_company")

	def replaceAmpersand(input: String): String =
		appConfig.ampersandRegex
			.replaceAllIn(input, "&amp;")
}