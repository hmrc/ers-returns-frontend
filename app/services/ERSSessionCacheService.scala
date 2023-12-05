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

package services

import config.{ApplicationConfig, ERSShortLivedCache}
import metrics.Metrics
import models._
import org.joda.time.DateTime
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.mvc.Request
import utils.{CountryCodes, HMACUtil, JsonParser, PageBuilder}
import repository.ERSSessionCacheRepository
import uk.gov.hmrc.mongo.cache.DataKey
import models.cache.CacheMap
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ERSSessionCacheService @Inject()(
                                        sessionCacheRepository: ERSSessionCacheRepository,
                                        val sessionService: SessionService,
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

  def cache[T](key: String, body: T)(implicit request: Request[_], formats: json.Format[T], hc: HeaderCarrier): Future[CacheMap] =
    sessionCacheRepository.putInSession[T](DataKey(key), body)

  def cache[T](key: String, body: T, sessionId: String)(implicit request: Request[_], formats: json.Format[T], hc: HeaderCarrier): Future[CacheMap] =
    sessionCacheRepository.putInSession[T](DataKey(key), body)

  def remove[T](key: String)(implicit request: Request[_]): Future[Unit] =
    sessionCacheRepository.deleteFromSession[T](DataKey(key))

  def fetch[T](key: String)(implicit request: Request[_], formats: json.Format[T], hc: HeaderCarrier): Future[T] = {
    //println(s"\n[1]Trying to fetch: $key\n")
    sessionCacheRepository.getFromSession[T](DataKey(key)).map { res =>
      //println(s"[1]Got from session: ${res.toString}\n")
      res.get
    } recover {
      case e: NoSuchElementException =>
        throw new NoSuchElementException
      case ex: Throwable =>
        println(ex.getMessage)
        throw new Exception
    }
  }

  def fetch[T](key: String, cacheId: String)(implicit request: Request[_], formats: json.Format[T], hc: HeaderCarrier): Future[T] = {
    println("Blah")
    val startTime = System.currentTimeMillis()
    println(s"\n[2]Trying to fetch: $key\n")
    sessionCacheRepository.getFromSession[JsValue](DataKey(key)).map { res =>
      println(s"[2]Got from session: ${res.toString}\n")
      cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
      res.get.as[T]
    } recover {
      case _: NoSuchElementException =>
        throw new NoSuchElementException
      case er: Throwable =>
        logger.warn(s"[ERSSessionCacheService][fetch] fetch (with 2 params) failed to get key [$key] for cacheId: [$cacheId] with exception ${er.getMessage}, " +
          s"timestamp: ${System.currentTimeMillis()}.")
        throw new Exception
    }
  }

  def fetchTrusteesOptionally(cacheId: String)(implicit request: Request[_], formats: json.Format[TrusteeDetailsList], hc: HeaderCarrier): Future[TrusteeDetailsList] = {
    fetch[TrusteeDetailsList](TRUSTEES_CACHE, cacheId).recover {
      case _ => TrusteeDetailsList(List.empty[TrusteeDetails])
    }
  }

  def fetchPartFromTrusteeDetailsList[T](index: Int, cacheId: String)(implicit request: Request[_], formats: json.Format[T]): Future[Option[T]] = {
    sessionCacheRepository.getFromSession[JsValue](DataKey(cacheId)).map { maybeJsValue =>
      maybeJsValue.flatMap { jsValue =>
        jsValue.asOpt[JsObject].flatMap { jsonObject =>
          jsonObject.value.get(TRUSTEES_CACHE).collect {
            case jsArray: JsArray =>
              jsArray.value.lift(index).map(_.as[T])
          }.flatten
        }
      }
    } recover {
      case x: Throwable =>
        logger.info("[ERSSessionCacheService][fetchPartFromTrusteeDetailsList] Nothing found in cache, expected if this is not an edit journey: " + x.getMessage)
        None
    }
  }

def fetchOption[T](key: String, cacheId: String)(implicit request: Request[_], hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
  val startTime = System.currentTimeMillis()
  println(s"\n[fetchOption]Trying to fetch: $key\n")
  sessionCacheRepository.getFromSession[JsValue](DataKey(key)).map { res =>
    println(s"[fetchOption]Got from session: ${res.toString}\n")
    cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
    res.map(_.as[T])
  } recover {
    case e: NoSuchElementException =>
      logger.warn(s"[cacheguy][fetchOption] fetch with 2 params failed to get key $key for $cacheId with exception \n $e \n timestamp: ${System.currentTimeMillis()}.")
      throw e
    case e: Throwable =>
      logger.error(s"[cacheguy][fetchOption] fetch with 2 params failed to get key $key for $cacheId, timestamp: ${System.currentTimeMillis()}.", e)
      throw new Exception
  }
}

  def fetchAll(sr: String)(implicit request: Request[_]): Future[CacheMap] = {
    val startTime = System.currentTimeMillis()

    sessionCacheRepository.getAllFromSession(request).map { maybeCacheMap =>
      cacheTimeFetch(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
      maybeCacheMap.getOrElse(CacheMap(sr, Map.empty))
    } recover {
      case e: Throwable =>
        logger.error(
          s"[String][fetchAll] failed to get all keys with exception \n $e \n Method: ${request.method} " +
            s"req: ${request.path}, param: ${request.rawQueryString}"
        )
        throw new Exception
    }
  }

  def getAltAmmendsData(schemeRef: String)(implicit request: Request[_], ec: ExecutionContext, hc: HeaderCarrier): Future[(Option[AltAmendsActivity], Option[AlterationAmends])] = {
    for {
      altAmendsActivity <- fetchOption[AltAmendsActivity](altAmendsActivity, schemeRef)
      altAmends <- {
        if (altAmendsActivity.exists(_.altActivity == OPTION_YES)) {
          fetchOption[AlterationAmends](ALT_AMENDS_CACHE_CONTROLLER, schemeRef)
        } else {
          Future.successful(None)
        }
      }
    } yield (altAmendsActivity, altAmends)
  }

  def getGroupSchemeData(schemeRef: String)(implicit request: Request[_], ec: ExecutionContext, hc: HeaderCarrier): Future[(Option[GroupSchemeInfo], Option[CompanyDetailsList])] = {
    for {
      groupSchemeInfo <- fetchOption[GroupSchemeInfo](GROUP_SCHEME_CACHE_CONTROLLER, schemeRef)
      companies <- {
        if (groupSchemeInfo.exists(_.groupScheme.contains(OPTION_YES))) {
          fetchOption[CompanyDetailsList](GROUP_SCHEME_COMPANIES, schemeRef)
        } else {
          Future.successful(None)
        }
      }
    } yield (groupSchemeInfo, companies)
  }


  def getAllData(bundleRef: String, ersMetaData: ErsMetaData)
                (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ErsSummary] = {
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

  def getNoOfRows(nilReturn: String)(implicit hc: HeaderCarrier): Future[Option[Int]] =
    if (isNilReturn(nilReturn)) {
      Future.successful(None)
    } else {
      sessionService.getSuccessfulCallbackRecord(hc: HeaderCarrier ).map(res => res.flatMap(_.noOfRows))
    }

  final def concatAddress(optionalAddressLines: List[Option[String]], existingAddressLines: String): String = {
    val definedStrings = optionalAddressLines.flatten
    existingAddressLines ++ definedStrings.map(addressLine => ", " + addressLine).mkString("")
  }

  def buildAddressSummary[T](entity: T): String = {
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
        val optionalAddressLines = List(
          trusteeDetails.addressLine2,
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
    } else {
      buildCompanyNameList(companyDetailsList, n + 1, companyNamesList + companyDetailsList(n).companyName + "<br>")
    }

  def buildTrusteeNameList(
                            trusteeDetailsList: List[TrusteeDetails],
                            n: Int = 0,
                            trusteeNamesList: String = ""
                          ): String =
    if (n == trusteeDetailsList.length) {
      trusteeNamesList
    } else {
      buildTrusteeNameList(trusteeDetailsList, n + 1, trusteeNamesList + trusteeDetailsList(n).name + "<br>")
    }

  private def getCacheId(implicit request: Request[_]): String =
  request.session.get("sessionId").getOrElse(throw new RuntimeException(""))


  def isNilReturn(nilReturn: String): Boolean = nilReturn == OPTION_NIL_RETURN

  def companyLocation(company: CompanyDetails): String =
    company.country
      .collect {
        case c if c != DEFAULT_COUNTRY => OVERSEAS
        case c => c
      }
      .getOrElse(DEFAULT)

  def trusteeLocationMessage(trustee: TrusteeDetails): String =
    if (trustee.basedInUk) "ers_trustee_based.uk" else "ers_trustee_based.overseas"

  def addCompanyMessage(messages: Messages, schemeOpt: Option[String]): String =
    messages.apply(s"ers_group_summary.${schemeOpt.getOrElse("").toLowerCase}.add_company")

  def replaceAmpersand(input: String): String =
    appConfig.ampersandRegex.replaceAllIn(input, "&amp;")

}