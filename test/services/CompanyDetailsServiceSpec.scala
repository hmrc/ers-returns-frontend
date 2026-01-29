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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers.{contain, convertToAnyShouldWrapper}
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Format
import utils.Fixtures.ersRequestObject
import utils.{ErsTestHelper, Fixtures}

import scala.concurrent.Future

class CompanyDetailsServiceSpec extends AnyWordSpecLike with ErsTestHelper {

  val companyOne: CompanyDetails   =
    CompanyDetails("First company", "20 Garden View", None, None, None, None, None, None, None, true)

  val companyTwo: CompanyDetails   =
    CompanyDetails("Second company", "72 Big Avenue", None, None, None, None, None, None, None, true)

  val companyThree: CompanyDetails =
    CompanyDetails("Third company", "21 Brick Lane", None, None, None, None, None, None, None, true)

  val companyFour: CompanyDetails  =
    CompanyDetails("Fourth company", "21 Brick Lane", None, None, None, None, None, None, None, true)

  val replacement: CompanyDetails  =
    CompanyDetails("Replacement company", "1 Some Place", None, None, None, None, None, None, None, true)

  val listOfCompanies: List[CompanyDetails] = List(
    companyOne,
    companyTwo,
    companyThree,
    companyFour
  )

  "calling replace company" must {
    val companyService: CompanyDetailsService = new CompanyDetailsService(mockErsUtil, mockSessionService)

    "add a new company to the list" when {
      "the index is 10000" in {

        val index = 10000

        val expectedOutput = List(
          companyOne,
          companyTwo,
          companyThree,
          companyFour,
          replacement
        )

        val result = companyService.replaceCompany(listOfCompanies, index, replacement)

        result shouldBe expectedOutput
      }
    }

    "replace a company and keep the other companies" when {

      "given an index that matches a company in the list" in {

        val index = 2

        val expectedOutput = List(
          companyOne,
          companyTwo,
          replacement,
          companyFour
        )

        val result = companyService.replaceCompany(listOfCompanies, index, replacement)

        result shouldBe expectedOutput
      }
    }

    "keep the existing list of companies" when {

      "given an index that does not match any existing companies" in {

        val index = 100

        val result = companyService.replaceCompany(listOfCompanies, index, replacement)

        result shouldBe listOfCompanies
        result shouldNot contain(replacement)
      }
    }

    "remove duplicate records" when {

      "duplicates are present" in {

        val index = 1

        val target =
          new CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None, true)

        val duplicateCompanyDetailsList = List(
          companyOne,
          target,
          target,
          target
        )

        val expectedOutput = List(
          companyOne,
          target
        )

        val result = companyService.replaceCompany(duplicateCompanyDetailsList, index, target)

        result shouldBe expectedOutput
      }
    }
  }

  "SchemeOrganiserCache" must {
    val companyDetailsService: CompanyDetailsService = new CompanyDetailsService(mockErsUtil, mockSessionService)

    "successfully update SchemeOrganiser cache" when {
      "scheme organiser details are fetched and updated" in {

        val companyName: Company           = Company("First Company", None, None)
        val companyAddress: CompanyAddress = Fixtures.companyAddressUK
        val cachedCompany                  = companyOne

        when(mockSessionService.fetch[RequestObject](any())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(
          mockSessionService.fetch[Company](eqTo(mockErsUtil.SCHEME_ORGANISER_NAME_CACHE))(any(), any[Format[Company]])
        )
          .thenReturn(Future.successful(companyName))
        when(mockSessionService.fetchSchemeOrganiserOptionally()(any(), any()))
          .thenReturn(Future.successful(Some(cachedCompany)))
        when(
          mockSessionService.fetch[CompanyAddress](eqTo(mockErsUtil.SCHEME_ORGANISER_ADDRESS_CACHE))(
            any(),
            eqTo(implicitly[Format[CompanyAddress]])
          )
        )
          .thenReturn(Future.successful(companyAddress))
        when(mockSessionService.cache[CompanyDetails](eqTo(mockErsUtil.SCHEME_ORGANISER_CACHE), any())(any(), any()))
          .thenReturn(Future.successful(sessionPair))

        val result: Unit = companyDetailsService.updateSchemeOrganiserCache

        result shouldBe ()
      }
    }

    "handle exceptions during fetching or caching" in {

      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      when(
        mockSessionService.fetch[Company](eqTo(mockErsUtil.SCHEME_ORGANISER_NAME_CACHE))(any(), any[Format[Company]])
      )
        .thenReturn(Future.failed(new Exception("Fetch failed")))

      val result: Unit = companyDetailsService.updateSchemeOrganiserCache

      result shouldBe ()
    }
  }

  "updateSubsidiaryCompanyCache" must {
    val companyDetailsService: CompanyDetailsService = new CompanyDetailsService(mockErsUtil, mockSessionService)

    "successfully update SubsidiaryCompany cache" when {
      "SubsidiaryCompany details are fetched and updated" in {
        val index                          = 1
        val companyName: Company           = Company("First Company", None, None)
        val companyAddress: CompanyAddress = Fixtures.companyAddressUK
        val cachedCompanies                = CompanyDetailsList(List(companyOne))

        when(mockSessionService.fetch[RequestObject](any())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(
          mockSessionService
            .fetch[Company](eqTo(mockErsUtil.SUBSIDIARY_COMPANY_NAME_CACHE))(any(), any[Format[Company]])
        )
          .thenReturn(Future.successful(companyName))
        when(mockSessionService.fetchCompaniesOptionally()(any(), any()))
          .thenReturn(Future.successful(cachedCompanies))
        when(
          mockSessionService.fetch[CompanyAddress](eqTo(mockErsUtil.SUBSIDIARY_COMPANY_ADDRESS_CACHE))(
            any(),
            eqTo(implicitly[Format[CompanyAddress]])
          )
        )
          .thenReturn(Future.successful(companyAddress))
        when(
          mockSessionService
            .cache[CompanyDetailsList](eqTo(mockErsUtil.SUBSIDIARY_COMPANIES_CACHE), any())(any(), any())
        )
          .thenReturn(Future.successful(sessionPair))

        val result: Unit = companyDetailsService.updateSubsidiaryCompanyCache(index)

        result shouldBe ()
      }
    }

    "handle exceptions during fetching or caching" in {
      val index = 1

      when(mockSessionService.fetch[RequestObject](any())(any(), any()))
        .thenReturn(Future.successful(ersRequestObject))

      when(
        mockSessionService.fetch[Company](eqTo(mockErsUtil.SCHEME_ORGANISER_NAME_CACHE))(any(), any[Format[Company]])
      )
        .thenReturn(Future.failed(new Exception("Fetch failed")))

      val result: Unit = companyDetailsService.updateSubsidiaryCompanyCache(index)

      result shouldBe ()
    }
  }

  "deleteCompany" must {
    val companyDetailsService: CompanyDetailsService = new CompanyDetailsService(mockErsUtil, mockSessionService)

    "return true" when
      Seq(
        ("the final company", CompanyDetailsList(List(companyOne))),
        ("multiple companies", CompanyDetailsList(listOfCompanies))
      ).foreach { case (numberCompanies, cachedCompanies) =>
        s"delete operation was successful with $numberCompanies" in {
          when(mockSessionService.fetch[RequestObject](any())(any(), any()))
            .thenReturn(Future.successful(ersRequestObject))
          when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))
          when(mockSessionService.fetchCompaniesOptionally()(any(), any()))
            .thenReturn(Future.successful(cachedCompanies))

          val result = companyDetailsService.deleteCompany(cachedCompanies, 0).futureValue

          result shouldBe true
        }
      }

    "return false" when {
      "delete operation failed" in {
        val cachedCompanies = CompanyDetailsList(listOfCompanies)

        when(mockSessionService.fetch[RequestObject](any())(any(), any()))
          .thenReturn(Future.successful(ersRequestObject))
        when(mockSessionService.cache(any(), any())(any(), any()))
          .thenReturn(Future.failed(new Exception("caching failed")))
        when(mockSessionService.fetchCompaniesOptionally()(any(), any())).thenReturn(Future.successful(cachedCompanies))

        val result = companyDetailsService.deleteCompany(cachedCompanies, 0).futureValue

        result shouldBe false
      }
    }
  }

}
