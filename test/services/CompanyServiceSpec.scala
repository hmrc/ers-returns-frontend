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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import utils.{ErsTestHelper, Fixtures}
import utils.Fixtures.ersRequestObject

import scala.concurrent.Future

class CompanyServiceSpec extends AnyWordSpecLike with ErsTestHelper {

  val company: CompanyDetails =
    CompanyDetails(Fixtures.companyName, "Address Line 1", None, None, None, Some("UK"), None, None, None)
  lazy val companyDetailsList: CompanyDetailsList = CompanyDetailsList(List(company, company))
  lazy val companyDetailsListSingle: CompanyDetailsList = CompanyDetailsList(List(company))

  "deleteCompany" must {
    val companyService: CompanyService = new CompanyService(mockErsUtil, mockSessionService)

    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))

    "return true" when {
      "delete operation was successful with multiple companies" in {
        when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
        when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsList))

        val result = companyService.deleteCompany(0).futureValue

        result shouldBe true
      }

      "delete operation was successful with final company" in {
        when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
        when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsListSingle))

        val result = companyService.deleteCompany(0).futureValue

        result shouldBe true
      }
    }

    "return false" when {
      "delete operation failed" in {
        when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.failed(new Exception("caching failed")))
        when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(companyDetailsList))

        val result = companyService.deleteCompany(0).futureValue

        result shouldBe false
      }
    }
  }
}
