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

import javax.inject.Inject
import models.{Company, CompanyAddress, CompanyDetails, CompanyDetailsList, RequestObject}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

class CompanyDetailsService @Inject()(
                                ersUtil: ERSUtil,
                                sessionService: FrontendSessionService
                              )(implicit ec: ExecutionContext) {

  def updateSubsidiaryCompanyCache(index: Int)(implicit request: Request[_]): Future[Unit] = {
    for {
      name <- sessionService.fetch[Company](ersUtil.SUBSIDIARY_COMPANY_NAME_CACHE)
      cachedCompanies <- sessionService.fetchCompaniesOptionally()
      companyDetailsList <- {
        sessionService.fetch[CompanyAddress](ersUtil.SUBSIDIARY_COMPANY_ADDRESS_CACHE).map(address =>
          CompanyDetailsList(replaceCompany(cachedCompanies.companies, index, CompanyDetails(name, address)))
        )
      }
      _ <- sessionService.cache[CompanyDetailsList](ersUtil.SUBSIDIARY_COMPANIES_CACHE, companyDetailsList)
    } yield {
      ()
    }
    }

  def updateSchemeOrganiserCache(implicit request: Request[_]): Future[Unit] = {
    for {
      requestObject <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      schemeRef = requestObject.getSchemeReference
      name <- sessionService.fetch[Company](ersUtil.SCHEME_ORGANISER_NAME_CACHE)
      companyDetails <- {
        sessionService.fetch[CompanyAddress](ersUtil.SCHEME_ORGANISER_ADDRESS_CACHE).map(address => {
          val x = CompanyDetails(name, address)
          println(s"We tryna cache these company details for scheme org:\n$x\n")
          x
        }
        )
      }
      _ <- sessionService.cache[CompanyDetails](ersUtil.SCHEME_ORGANISER_CACHE, companyDetails)
    } yield {
      ()
    }
  }


    def replaceCompany(companies: List[CompanyDetails], index: Int, formData: CompanyDetails): List[CompanyDetails] =
      (if (index == 10000) {
        println(s"\n\n[${this.getClass.getSimpleName}] index is $index ")
        companies :+ formData
      } else {
        println(s"\n\n[${this.getClass.getSimpleName}] index is $index")
        companies.zipWithIndex.map {
          case (a, b) => if (b == index) formData else a
        }
      }).distinct
  }
