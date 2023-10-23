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
import models.{CompanyAddress, CompanyBasedInUk, CompanyDetails, CompanyDetailsList, Company, RequestObject}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ERSUtil

import scala.concurrent.{ExecutionContext, Future}

class CompanyDetailsService @Inject()(
                                ersUtil: ERSUtil
                              )(implicit ec: ExecutionContext) {

  def updateCompanyCache(index: Int)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      schemeRef = requestObject.getSchemeReference
      name <- ersUtil.fetch[Company](ersUtil.COMPANY_NAME_CACHE, schemeRef)
      cachedCompanies <- ersUtil.fetchCompaniesOptionally(schemeRef)
      companyDetailsList <- {
        ersUtil.fetch[CompanyAddress](ersUtil.COMPANY_ADDRESS_CACHE, schemeRef).map(address =>
          CompanyDetailsList(replaceCompany(cachedCompanies.companies, index, CompanyDetails(name, address)))
        )
      }
      _ <- ersUtil.cache[CompanyDetailsList](ersUtil.COMPANIES_CACHE, companyDetailsList, schemeRef)
    } yield {
      Unit
    }
  }


  def replaceCompany(companies: List[CompanyDetails], index: Int, formData: CompanyDetails): List[CompanyDetails] =
    (if (index == 10000) {
      println(s"\n\n[${this.getClass.getSimpleName}] index is $index ")
      companies :+ formData
    } else {
      println(s"\n\n[${this.getClass.getSimpleName}] index is $index")
      companies.zipWithIndex.map{
        case (a, b) => if (b == index) formData else a
      }
    }).distinct
}