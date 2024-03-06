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

package utils

import config.{ApplicationConfig, ERSShortLivedCache}
import metrics.Metrics
import models._
import org.joda.time.DateTime
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.Request
import services.{FrontendSessionService, SessionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ERSUtil @Inject() (
  val sessionService: FrontendSessionService,
  val shortLivedCache: ERSShortLivedCache,
  val appConfig: ApplicationConfig
)(implicit val ec: ExecutionContext, countryCodes: CountryCodes)
    extends PageBuilder
    with JsonParser
    with Metrics
    with HMACUtil
			with Constants
    with Logging {

  val largeFileStatus                       = "largefiles"
  val savedStatus                           = "saved"
  val ersMetaData: String                   = "ErsMetaData"
  val ersRequestObject: String              = "ErsRequestObject"
  val reportableEvents                      = "ReportableEvents"
  val csvFilesCallbackList: String          = "csv-file-callback-List"

  // Cache Ids
  val altAmendsActivity: String       = "alt-activity"

	val TRUSTEE_ADDRESS_UK_CACHE: String = "trustee-address-uk"
	val TRUSTEE_ADDRESS_OVERSEAS_CACHE: String = "trustee-address-overseas"
	val ERROR_REPORT_DATETIME: String = "error-report-datetime"

  // Params
  val PORTAL_AOREF_CACHE: String          = "portal-ao-ref"
  val PORTAL_TAX_YEAR_CACHE: String       = "portal-tax-year"
  val PORTAL_ERS_SCHEME_REF_CACHE: String = "portal-ers-scheme-ref"
  val PORTAL_SCHEME_TYPE: String          = "portal-scheme-type"
  val PORTAL_SCHEME_NAME_CACHE: String    = "portal-scheme-name"
  val PORTAL_HMAC_CACHE: String           = "portal-hmac"
  val PORTAL_SCHEME_REF: String           = "portal-scheme-ref"

  val CONFIRMATION_DATETIME_CACHE: String = "confirmation-date-time"

  // new cache amends
  val PORTAL_PARAMS_CACHE: String = "portal_params"

  val FILE_TRANSFER_CACHE      = "file-tansfer-cache"
  val FILE_TRANSFER_CACHE_LIST = "file-transfer-cache-list"
  val IP_REF: String           = "ip-ref"

	val VALIDATED_SHEEETS: String = "validated-sheets"

	def cache[T](key: String, body: T)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: json.Format[T]): Future[CacheMap] =
		shortLivedCache.cache[T](getCacheId, key, body)

	def cache[T](key: String, body: T, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[CacheMap] = {
		logger.info(s"[ERSUtil][cache]cache saving key:$key, cacheId:$cacheId")
		println(s"\n We are Caching:\n$body\nto key: $key")
		shortLivedCache.cache[T](cacheId, key, body)
	}

	def remove(cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
		shortLivedCache.remove(cacheId)

	def fetch[T](key: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: json.Format[T]): Future[T] = {
		shortLivedCache.fetchAndGetEntry[JsValue](getCacheId, key).map { res =>
			res.get.as[T]
		} recover {
			case _: NoSuchElementException =>
				throw new NoSuchElementException
			case _: Throwable =>
				logger.error(s"[ERSUtil][fetch] fetch failed to get key $key for $getCacheId with exception, timestamp: ${System.currentTimeMillis()}.")
				throw new Exception
		}
	}

	def fetch[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[T] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetchAndGetEntry[JsValue](cacheId, key).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res.get.as[T]
		} recover {
			case _: NoSuchElementException =>
				throw new NoSuchElementException
			case er: Throwable =>
				logger.warn(s"[ERSUtil][fetch] fetch(with 2 params) failed to get key [$key] for cacheId: [$cacheId] with exception ${er.getMessage}, " +
					s"timestamp: ${System.currentTimeMillis()}.")
				throw new Exception
		}
	}

	def fetchTrusteesOptionally(cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[TrusteeDetailsList]): Future[TrusteeDetailsList] = {
		fetch[TrusteeDetailsList](TRUSTEES_CACHE, cacheId).recover {
			case _ => TrusteeDetailsList(List.empty[TrusteeDetails])
		}
	}

	def fetchPartFromTrusteeDetailsList[A](index: Int, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[A]): Future[Option[A]] = {
		shortLivedCache.fetchAndGetEntry[JsValue](cacheId, TRUSTEES_CACHE).map {
			_.map(_.\(TRUSTEES_CACHE).as[JsArray].\(index).getOrElse(Json.obj()).as[A])
		} recover {
			case x: Throwable => {
				logger.info("[ERSUtil][fetchPartFromTrusteeDetailsList] Nothing found in cache, expected if this is not an edit journey: " + x.getMessage)
				None
			}
		}
	}

	//TODO Why is this called "fetchOption" if it throws an expection when there's no data?? It can only return Some[T] or throw exception which I would not describe as "optional" behaviour -.-
	def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetchAndGetEntry[T](cacheId, key).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res
		} recover {
			case e: NoSuchElementException =>
				logger.warn(s"[ERSUtil][fetchOption] fetch with 2 params failed to get key $key for $cacheId with exception \n $e \n timestamp: ${System.currentTimeMillis()}.")
				throw e
			case e: Throwable =>
				logger.error(s"[ERSUtil][fetchOption] fetch with 2 params failed to get key $key for $cacheId, timestamp: ${System.currentTimeMillis()}.", e)
				throw new Exception
		}
	}

	def actuallyFetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetchAndGetEntry[T](cacheId, key).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res
		} recover {
			case _ => None
		}
	}

	def fetchAll(sr: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[CacheMap] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetch(sr).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res.get
		} recover {
			case e: NoSuchElementException =>
				logger.warn(
					s"[ERSUtil][fetchAll] failed to get all keys with a NoSuchElementException \n $e \n Method: ${request.method} " +
						s"req: ${request.path}, param: ${request.rawQueryString}"
				)
				throw new NoSuchElementException
			case e: Throwable =>
				logger.error(
					s"[ERSUtil][fetchAll] failed to get all keys with exception \n $e \n Method: ${request.method} " +
						s"req: ${request.path}, param: ${request.rawQueryString}"
				)
				throw new Exception
		}
	}

	def getAltAmmendsData(schemeRef: String)(implicit hc: HeaderCarrier,
																					 ec: ExecutionContext
	): Future[(Option[AltAmendsActivity], Option[AlterationAmends])] = {
		fetchOption[AltAmendsActivity](altAmendsActivity, schemeRef).flatMap {
			altamends =>
				if (altamends.getOrElse(AltAmendsActivity("")).altActivity == OPTION_YES) {
					fetchOption[AlterationAmends](ALT_AMENDS_CACHE_CONTROLLER, schemeRef).map {
						amc =>
							(altamends, amc)
					}
				}
				else {
					Future{
						(altamends, None)
					}
				}
		}
	}

	def getGroupSchemeData(schemeRef: String)
												(implicit hc: HeaderCarrier,
												 ec: ExecutionContext): Future[(Option[GroupSchemeInfo], Option[CompanyDetailsList])] = {
		fetchOption[GroupSchemeInfo](GROUP_SCHEME_CACHE_CONTROLLER, schemeRef).flatMap { gsc =>
			if (gsc.getOrElse(GroupSchemeInfo(None, None)).groupScheme.getOrElse("") == OPTION_YES) {
				fetchOption[CompanyDetailsList](GROUP_SCHEME_COMPANIES, schemeRef).map { comp =>
					(gsc, comp)
				}
			}
			else {
				Future {
					(gsc, None)
				}
			}
		}
	}


	def getAllData(bundleRef: String, ersMetaData: ErsMetaData)
								(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ErsSummary] = {
		val schemeRef = ersMetaData.schemeInfo.schemeRef
		(for {
			repEvents <- fetchOption[ReportableEvents](reportableEvents, schemeRef)
			checkFileType <- fetchOption[CheckFileType](FILE_TYPE_CACHE, schemeRef)
			soc <- fetchOption[SchemeOrganiserDetails](SCHEME_ORGANISER_CACHE, schemeRef)
			td <- fetchOption[TrusteeDetailsList](TRUSTEES_CACHE, schemeRef)
			gc <- getGroupSchemeData(schemeRef)
			altData <- getAltAmmendsData(schemeRef)
			trows <- getNoOfRows(repEvents.get.isNilReturn.get)
		} yield {
			val fileType = checkFileType.map(_.checkFileType.get)
			ErsSummary(bundleRef, repEvents.get.isNilReturn.get, fileType, DateTime.now, metaData = ersMetaData,
				altAmendsActivity = altData._1, alterationAmends = altData._2, groupService = gc._1,
				schemeOrganiser = soc, companies = gc._2, trustees = td, nofOfRows = trows, transferStatus = getStatus(trows)
			)
		}
			).recover {
			//TODO I don't like this. Pick up in tech debt review
			case e: NoSuchElementException =>
				logger.error(s"[ERSUtil][getAllData]: Get all data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
				throw e
		}
	}

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

	def companyLocation(company: CompanyDetails): String = if (company.basedInUk) DEFAULT_COUNTRY else OVERSEAS

	def trusteeLocationMessage(trustee: TrusteeDetails): String = if (trustee.basedInUk) "ers_trustee_based.uk" else "ers_trustee_based.overseas"

	def addCompanyMessage(messages: Messages, schemeOpt: Option[String]): String =
		messages.apply(s"ers_group_summary.${schemeOpt.getOrElse("").toLowerCase}.add_company")

	def replaceAmpersand(input: String): String = {
		appConfig.ampersandRegex
			.replaceAllIn(input, "&amp;")
	}
}
