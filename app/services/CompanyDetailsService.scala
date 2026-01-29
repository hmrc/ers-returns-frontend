/*
 * Copyright 2026 HM Revenue & Customs
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

import models._
import play.api.Logging
import play.api.mvc.RequestHeader
import utils.ERSUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyDetailsService @Inject() (
  ersUtil: ERSUtil,
  sessionService: FrontendSessionService
)(implicit ec: ExecutionContext)
    extends Logging {

  private def filterOutDeletedCompany(companyList: CompanyDetailsList, indexToDelete: Int): List[CompanyDetails] =
    companyList.companies.zipWithIndex.filterNot(_._2 == indexToDelete).map(_._1)

  def deleteCompany(companies: CompanyDetailsList, index: Int)(implicit request: RequestHeader): Future[Boolean] = {
    val activeCompanies = CompanyDetailsList(filterOutDeletedCompany(companies, index))
    (for {
      _ <- sessionService.cache(ersUtil.SUBSIDIARY_COMPANIES_CACHE, activeCompanies)
    } yield {
      if (activeCompanies.companies.isEmpty)
        sessionService.cache(ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, GroupSchemeInfo(None, None))
      true
    }).recover { case e: Throwable =>
      logger.warn(s"[CompanyDetailsService][deleteCompany] Deleting company failed: ${e.getMessage}")
      false
    }
  }

  def updateSubsidiaryCompanyCache(index: Int)(implicit request: RequestHeader): Unit =
    try
      sessionService
        .fetch[Company](ersUtil.SUBSIDIARY_COMPANY_NAME_CACHE)
        .map(name =>
          sessionService
            .fetchCompaniesOptionally()
            .map(cachedCompanies =>
              sessionService
                .fetch[CompanyAddress](ersUtil.SUBSIDIARY_COMPANY_ADDRESS_CACHE)
                .map(address =>
                  CompanyDetailsList(replaceCompany(cachedCompanies.companies, index, CompanyDetails(name, address)))
                )
                .map(companyDetailsList =>
                  sessionService.cache[CompanyDetailsList](ersUtil.SUBSIDIARY_COMPANIES_CACHE, companyDetailsList)
                )
            )
        )
    catch {
      case ex: Throwable =>
        logger.error(
          "[CompanyDetailsService][updateSubsidiaryCompanyCache] Error updating subsidiary company cache",
          ex
        )
    }

  def updateSchemeOrganiserCache(implicit request: RequestHeader): Unit =
    try
      sessionService
        .fetch[Company](ersUtil.SCHEME_ORGANISER_NAME_CACHE)
        .map(name =>
          sessionService
            .fetch[CompanyAddress](ersUtil.SCHEME_ORGANISER_ADDRESS_CACHE)
            .map(address =>
              sessionService.cache[CompanyDetails](ersUtil.SCHEME_ORGANISER_CACHE, CompanyDetails(name, address))
            )
        )
    catch {
      case ex: Throwable =>
        logger.error("[CompanyDetailsService][updateSchemeOrganiserCache] Error updating scheme organiser cache", ex)
    }

  def replaceCompany(companies: List[CompanyDetails], index: Int, formData: CompanyDetails): List[CompanyDetails] =
    (if (index == 10000) {
       companies :+ formData
     } else {
       companies.zipWithIndex.map { case (a, b) =>
         if (b == index) formData else a
       }
     }).distinct

}
