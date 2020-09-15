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

import java.util.concurrent.TimeUnit

import config.{ApplicationConfig, ERSShortLivedCache, ERSShortLivedHttpCache}
import javax.inject.{Inject, Singleton}
import metrics.Metrics
import models.{ErsMetaData, ErsSummary, GroupSchemeInfo, ReportableEvents, SchemeOrganiserDetails, TrusteeDetailsList}
import models.{AltAmendsActivity, AlterationAmends, CheckFileType, CompanyDetailsList}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json
import play.api.libs.json.JsValue
import play.api.mvc.Request
import services.SessionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ERSUtil @Inject()(val sessionService: SessionService,
												val shortLivedCache: ERSShortLivedCache,
											  val appConfig: ApplicationConfig
											 ) extends PageBuilder with JsonParser with Metrics with HMACUtil {

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

	def cache[T](key:String, body:T)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]): Future[CacheMap] =
		shortLivedCache.cache[T](getCacheId, key, body)

	def cache[T](key: String, body: T, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[CacheMap] = {
		Logger.info(s"[ERSUtil][cache]cache saving key:$key, cacheId:$cacheId")
		shortLivedCache.cache[T](cacheId, key, body)
	}

	def remove(cacheId: String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] =
		shortLivedCache.remove(cacheId)

	def fetch[T](key:String)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]): Future[T] = {
		shortLivedCache.fetchAndGetEntry[JsValue](getCacheId, key).map{ res =>
			res.get.as[T]
		}recover{
			case e: NoSuchElementException =>
				Logger.error(s"[ERSUtil][fetch] fetch failed to get key $key with exception $e, timestamp: ${System.currentTimeMillis()}.")
				throw new NoSuchElementException
			case _ : Throwable =>
				Logger.error(s"[ERSUtil][fetch] fetch failed to get key $key for $getCacheId with exception, timestamp: ${System.currentTimeMillis()}.")
				throw new Exception
		}
	}

	def fetch[T](key: String, cacheId: String)(implicit hc: HeaderCarrier,  formats: json.Format[T], request: Request[AnyRef]): Future[T] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetchAndGetEntry[JsValue](cacheId, key).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res.get.as[T]
		} recover {
			case e: NoSuchElementException =>
				Logger.error(s"[ERSUtil][fetch] fetch(with 2 params) failed to get key [$key] for cacheId: [$cacheId] with exception $e, timestamp: ${System.currentTimeMillis()}.")
				throw new NoSuchElementException
			case er : Throwable =>
				Logger.error(s"[ERSUtil][fetch] fetch(with 2 params) failed to get key [$key] for cacheId: [$cacheId] with exception ${er.getMessage}, " +
					s"timestamp: ${System.currentTimeMillis()}.")
				throw new Exception
		}
	}

	def fetchOption[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T], request: Request[AnyRef]): Future[Option[T]] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetchAndGetEntry[T](cacheId, key).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res
		} recover {
			case e: NoSuchElementException =>
				Logger.error(s"[ERSUtil][fetchOption] fetch with 2 params failed to get key $key for $cacheId with exception \n $e \n timestamp: ${System.currentTimeMillis()}.")
				throw e
			case e: Throwable =>
				Logger.error(s"[ERSUtil][fetchOption] fetch with 2 params failed to get key $key for $cacheId, timestamp: ${System.currentTimeMillis()}.", e)
				throw new Exception
		}
	}

	def fetchAll(sr: String)(implicit hc: HeaderCarrier, request: Request[AnyRef]): Future[CacheMap] = {
		val startTime = System.currentTimeMillis()
		shortLivedCache.fetch(sr).map { res =>
			cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
			res.get
		} recover {
			case e: NoSuchElementException =>
				Logger.error(s"[ERSUtil][fetchAll] failed to get all keys with a NoSuchElementException \n $e \n Method: ${request.method} " +
					s"req: ${request.path}, param: ${request.rawQueryString}")
				throw new NoSuchElementException
			case e: Throwable =>
				Logger.error(s"[ERSUtil][fetchAll] failed to get all keys with exception \n $e \n Method: ${request.method} " +
					s"req: ${request.path}, param: ${request.rawQueryString}")
				throw new Exception
		}
	}

	def getAltAmmendsData(schemeRef: String)(implicit hc: HeaderCarrier,
																					 ec: ExecutionContext,
																					 request: Request[AnyRef]
																					): Future[(Option[AltAmendsActivity], Option[AlterationAmends])] = {
		fetchOption[AltAmendsActivity](altAmendsActivity, schemeRef).flatMap {
			altamends =>
				if(altamends.getOrElse(AltAmendsActivity("")).altActivity == OPTION_YES) {
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
												 ec: ExecutionContext,
												 request: Request[AnyRef]): Future[(Option[GroupSchemeInfo], Option[CompanyDetailsList])] = {
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
								(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[AnyRef]): Future[ErsSummary] = {
		val schemeRef = ersMetaData.schemeInfo.schemeRef
		fetchOption[ReportableEvents](reportableEvents, schemeRef).flatMap { repEvents =>
			fetchOption[CheckFileType](FILE_TYPE_CACHE, schemeRef).flatMap { checkFileType =>
				fetchOption[SchemeOrganiserDetails](SCHEME_ORGANISER_CACHE, schemeRef).flatMap { soc =>
					fetchOption[TrusteeDetailsList](TRUSTEES_CACHE, schemeRef).flatMap { td =>
						getGroupSchemeData(schemeRef).flatMap { gc =>
							getAltAmmendsData(schemeRef).flatMap { altData =>
								getNoOfRows(repEvents.get.isNilReturn.get).map { trows =>
									val fileType = if (checkFileType.isDefined) {
										Some(checkFileType.get.checkFileType.get)
									} else {
										None
									}
									new ErsSummary(bundleRef, repEvents.get.isNilReturn.get, fileType, DateTime.now, metaData = ersMetaData,
										altAmendsActivity = altData._1, alterationAmends = altData._2, groupService = gc._1,
										schemeOrganiser = soc, companies = gc._2, trustees = td, nofOfRows = trows, transferStatus = getStatus(trows) )
								}
							}
						}
					}
				}
			}
		}.recover {
			case e: NoSuchElementException =>
				Logger.error(s"CacheUtil: Get all data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
				throw new Exception
		}
	}

	def getStatus(tRows: Option[Int]): Some[String] =
		if (tRows.isDefined && tRows.get > appConfig.sentViaSchedulerNoOfRowsLimit) {
			Some(largeFileStatus)
		} else {
			Some(savedStatus)
		}

	def getNoOfRows(nilReturn:String)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[AnyRef]): Future[Option[Int]] = {
		if (isNilReturn(nilReturn: String)) {
			Future.successful(None)
		} else {
			sessionService.getSuccessfulCallbackRecord.map(res => res.flatMap(_.noOfRows))
		}
	}

	private def getCacheId (implicit hc: HeaderCarrier): String = {
		hc.sessionId.getOrElse(throw new RuntimeException("")).value
	}

	def isNilReturn(nilReturn:String) :Boolean = nilReturn == OPTION_NIL_RETURN
}
