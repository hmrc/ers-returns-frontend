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

import models._
import play.api.Logging
import play.api.mvc.Request
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompanyService @Inject()(ersUtil: ERSUtil,
                               sessionService: FrontendSessionService
                              )(implicit ec: ExecutionContext) extends Logging {

  private def filterDeletedCompany(companyList: CompanyDetailsList, id: Int): List[CompanyDetails] =
    companyList.companies.zipWithIndex.filterNot(_._2 == id).map(_._1)

  def deleteCompany(index: Int)(implicit request: Request[_]): Future[Boolean] = {
    (for {
      companies <- sessionService.fetchCompaniesOptionally()
      companyDetailsList = CompanyDetailsList(filterDeletedCompany(companies, index))
      companySize = companies.companies.size
      _ <- sessionService.cache(ersUtil.GROUP_SCHEME_COMPANIES, companyDetailsList)
    } yield {
      if (companySize == 1) sessionService.cache(ersUtil.GROUP_SCHEME_CACHE_CONTROLLER, GroupSchemeInfo(None, None))
      true
    }).recover {
      case e: Throwable =>
        logger.warn(s"[CompanyService][deleteCompany] Deleting company failed: ${e.getMessage}")
        false
    }
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
