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

package services

import config.ApplicationConfig
import controllers.auth.RequestWithOptionalAuthContext
import models._
import play.api.Logging
import play.api.libs.json
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import repositories.FrontendSessionsRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import utils.JsonUtils._
import utils.{Constants, PageBuilder}

import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FrontendSessionService @Inject()(val sessionCache: FrontendSessionsRepository,
                                       fileValidatorService: FileValidatorService,
                                       applicationConfig: ApplicationConfig)(implicit ec: ExecutionContext) extends PageBuilder with Logging with Constants {

  def cache[T](key: String, body: T)(implicit request: Request[_], formats: json.Format[T]): Future[(String, String)] =
    sessionCache.putSession[T](DataKey(key), body).recoverWith {
      case ex: Exception =>
        logger.error(s"Failed to cache data for key $key", ex)
        Future.failed(ex)
    }

  def remove(key: String)(implicit request: Request[_]): Future[Unit] =
    sessionCache.deleteFromSession(DataKey(key)).recoverWith {
      case ex: Exception =>
        logger.error(s"Failed to remove data for key $key from cache, Exception: ${ex.getMessage}", ex)
        Future.failed(ex)
    }

  def fetch[T](key: String)(implicit request: Request[_], formats: json.Format[T]): Future[T] = {
    sessionCache.getFromSession[JsValue](DataKey(key)).map { result =>
      result.get.as[T] //to be picked up in tech debt review
    } recoverWith {
      case _: NoSuchElementException =>
        throw new NoSuchElementException(s"[FrontendSessionService][fetch] No data found for key $key")
      case ex: Exception =>
        val errMsg = s"[FrontendSessionService][fetch] Fetch failed for key $key in session ${request.session.get("sessionId").getOrElse("no session id")} with exception ${ex.getMessage}, timestamp: ${System.currentTimeMillis()}."
        logger.error(errMsg, ex)
        Future.failed(ex)
    }
  }

  def fetchTrusteesOptionally()(implicit request: Request[_], formats: json.Format[TrusteeDetailsList]): Future[TrusteeDetailsList] = {
    fetch[TrusteeDetailsList](TRUSTEES_CACHE).recoverWith {
      case ex: Exception =>
        logger.warn(s"[FrontendSessionService][fetchTrusteesOptionally] Failed to fetch trustees, returning empty list. Error: ${ex.getMessage}")
        Future.successful(TrusteeDetailsList(List.empty[TrusteeDetails]))
    }
  }



  def fetchPartFromTrusteeDetailsList[A](index: Int)(implicit request: Request[_], formats: json.Format[A]): Future[Option[A]] = {
    sessionCache.getFromSession[JsValue](DataKey(TRUSTEES_CACHE)).map { optionalJson =>
      optionalJson.flatMap { json =>
        (json \ TRUSTEES_CACHE).as[JsArray].applyOption(index).map(_.as[A])
      }
    } recover {
      case ex: Exception =>
        logger.info(s"[FrontendSessionService][fetchPartFromTrusteeDetailsList] Nothing found in cache or error in processing for index $index, which may be expected for a non-edit journey: ${ex.getMessage}")
        None
    }
  }

  def fetchOption[T](key: String, cacheId: String)(implicit request: Request[_], formats: json.Format[T]): Future[Option[T]] = {
    sessionCache.getFromSession[T](DataKey(key)).recoverWith {
      case e: NoSuchElementException =>
        logger.warn(s"[FrontendSessionService][fetchOption] No data found for key $key in cache $cacheId. Exception: ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        Future.successful(None)
      case e: Throwable =>
        logger.error(s"[FrontendSessionService][fetchOption] Error fetching key $key from cache $cacheId, timestamp: ${System.currentTimeMillis()}.", e)
        Future.successful(None)
    }
  }

  def fetchAll()(implicit request: Request[_]): Future[CacheItem] = {
    sessionCache.getAllFromSession().flatMap {
      case Some(cacheItem) => Future.successful(cacheItem)
      case None =>
        val errMsg = s"[FrontendSessionService][fetchAll] No data found in session. Method: ${request.method}, req: ${request.path}, param: ${request.rawQueryString}"
        logger.warn(errMsg)
        Future.failed(new NoSuchElementException(errMsg))
    } recoverWith {
      case e: Throwable =>
        val errMsg = s"[FrontendSessionService][fetchAll] Error fetching all keys. Method: ${request.method}, req: ${request.path}, param: ${request.rawQueryString}, Exception: ${e.getMessage}"
        logger.error(errMsg, e)
        Future.failed(new Exception(errMsg))
    }
  }

  def getAltAmmendsData(schemeRef: String)
                       (implicit ec: ExecutionContext, request: Request[_]): Future[(Option[AltAmendsActivity], Option[AlterationAmends])] = {
    for {
      altAmendsOption <- fetchOption[AltAmendsActivity](ALT_AMENDS_ACTIVITY, schemeRef)
      alterationAmendsOption <- altAmendsOption match {
        case Some(altAmends) if altAmends.altActivity == OPTION_YES =>
          fetchOption[AlterationAmends](ALT_AMENDS_CACHE_CONTROLLER, schemeRef)
        case _ =>
          Future.successful(None)
      }
    } yield (altAmendsOption, alterationAmendsOption)
  }

  def getGroupSchemeData(schemeRef: String)
                        (implicit request: Request[_], ec: ExecutionContext): Future[(Option[GroupSchemeInfo], Option[CompanyDetailsList])] = {
    for {
      gscOption <- fetchOption[GroupSchemeInfo](GROUP_SCHEME_CACHE_CONTROLLER, schemeRef)
      companyDetailsOption <- gscOption match {
        case Some(gsc) if gsc.groupScheme.contains(OPTION_YES) =>
          fetchOption[CompanyDetailsList](GROUP_SCHEME_COMPANIES, schemeRef)
        case _ =>
          Future.successful(None)
      }
    } yield (gscOption, companyDetailsOption)
  }

  def getAllData(bundleRef: String, ersMetaData: ErsMetaData)
                (implicit ec: ExecutionContext, request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[ErsSummary] = {
    val schemeRef = ersMetaData.schemeInfo.schemeRef
    (for {
      repEvents <- fetchOption[ReportableEvents](REPORTABLE_EVENTS, schemeRef)
      checkFileType <- fetchOption[CheckFileType](FILE_TYPE_CACHE, schemeRef)
      soc <- fetchOption[SchemeOrganiserDetails](SCHEME_ORGANISER_CACHE, schemeRef)
      td <- fetchOption[TrusteeDetailsList](TRUSTEES_CACHE, schemeRef)
      gc <- getGroupSchemeData(schemeRef)
      altData <- getAltAmmendsData(schemeRef)
      trows <- getNoOfRows(repEvents.get.isNilReturn.get)
    } yield {
      val fileType = checkFileType.map(_.checkFileType.get)
      ErsSummary(bundleRef, repEvents.get.isNilReturn.get, fileType, ZonedDateTime.now, metaData = ersMetaData,
        altAmendsActivity = altData._1, alterationAmends = altData._2, groupService = gc._1,
        schemeOrganiser = soc, companies = gc._2, trustees = td, nofOfRows = trows, transferStatus = getStatus(trows)
      )
    }
      ).recover {
      case e: NoSuchElementException =>
        logger.error(s"[FrontendSessionService][getAllData]: Get all data from cache failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.", e)
        throw e
    }
  }

  def getNoOfRows(nilReturn: String)(implicit ec: ExecutionContext, request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Option[Int]] =
    if (isNilReturn(nilReturn: String)) {
      Future.successful(None)
    } else {
      fileValidatorService.getSuccessfulCallbackRecord.map(res => res.flatMap(_.noOfRows))
    }

  def isNilReturn(nilReturn: String): Boolean = nilReturn == OPTION_NIL_RETURN

  def getStatus(tRows: Option[Int]): Some[String] =
    if (tRows.isDefined && tRows.get > applicationConfig.sentViaSchedulerNoOfRowsLimit) {
      Some(LARGE_FILE_STATUS)
    } else {
      Some(SAVED_STATUS)
    }

  def fetchPartFromCompanyDetailsList[A](index: Int)(implicit request: Request[_], formats: json.Format[A]): Future[Option[A]] = {
    sessionCache.getFromSession[JsValue](DataKey(SUBSIDIARY_COMPANIES_CACHE)).map {
      subsidiaryCompanies =>
        subsidiaryCompanies.map(_.\(COMPANIES).as[JsArray].\(index).getOrElse(Json.obj()).as[A])
    } recover {
      case x: Throwable => {
        logger.debug("[ERSUtil][fetchPartFromCompanyDetailsList] Nothing found in cache, expected if this is not an edit journey: " + x.getMessage)
        None
      }
    }
  }



  def fetchPartFromCompanyDetails[A]()(implicit request: Request[_], formats: json.Format[A]): Future[Option[A]] = {
    sessionCache.getFromSession[JsValue](DataKey(SCHEME_ORGANISER_CACHE)).map {
      companyDetailsOpt =>
        companyDetailsOpt.map(_.as[A])
    } recover {
      case x: Throwable => {
        logger.debug("[ERSUtil][fetchPartFromCompanyDetailsList] Nothing found in cache, expected if this is not an edit journey: " + x.getMessage)
        None
      }
    }
  }

  def fetchCompaniesOptionally()(implicit request: Request[_], formats: json.Format[CompanyDetailsList]): Future[CompanyDetailsList] = {
    fetch[CompanyDetailsList](SUBSIDIARY_COMPANIES_CACHE).recover {
      case _ => CompanyDetailsList(List.empty[CompanyDetails])
    }
  }

  def fetchSchemeOrganiserOptionally()(implicit request: Request[_], formats: json.Format[CompanyDetails]): Future[Option[CompanyDetails]]= {
    fetch[CompanyDetails](SCHEME_ORGANISER_CACHE).map(Some(_)).recover {
      case _ => None
    }
  }
}
