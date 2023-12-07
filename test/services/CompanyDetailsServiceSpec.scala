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

import models.CompanyDetails
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import utils.ErsTestHelper

class CompanyDetailsServiceSpec extends AnyWordSpecLike with ErsTestHelper {

  "calling replace company" must {
    val companyService: CompanyDetailsService = new CompanyDetailsService(mockErsUtil)

    val companyOne   = CompanyDetails("First company", "20 Garden View", None, None, None, None, None, None, None, true)
    val companyTwo   = CompanyDetails("Second company", "72 Big Avenue", None, None, None, None, None, None, None, true)
    val companyThree = CompanyDetails("Third company", "21 Brick Lane", None, None, None, None, None, None, None, true)
    val companyFour  = CompanyDetails("Fourth company", "21 Brick Lane", None, None, None, None, None, None, None, true)
    val replacement  = CompanyDetails("Replacement company", "1 Some Place", None, None, None, None, None, None, None, true)

    val CompanyDetailsList = List(
      companyOne,
      companyTwo,
      companyThree,
      companyFour
    )

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

        val result = companyService.replaceCompany(CompanyDetailsList, index, replacement)

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

        val result = companyService.replaceCompany(CompanyDetailsList, index, replacement)

        result shouldBe expectedOutput
      }
    }

    "keep the existing list of companies" when {

      "given an index that does not match any existing companies" in {

        val index = 100

        val result = companyService.replaceCompany(CompanyDetailsList, index, replacement)

        result shouldBe CompanyDetailsList
      }
    }

    "remove duplicate records" when {

      "duplicates are present" in {

        val index = 1

        val target = new CompanyDetails("Target Company", "3 Window Close", None, None, None, None, None, None, None, true)

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
}