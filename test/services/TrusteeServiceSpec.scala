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

import models.{RequestObject, TrusteeDetails, TrusteeDetailsList}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import utils.ErsTestHelper
import utils.Fixtures.{ersRequestObject, exampleTrustees}

import scala.concurrent.Future

class TrusteeServiceSpec extends AnyWordSpecLike with ErsTestHelper {

  "calling replace trustee" must {
    val trusteeService: TrusteeService = new TrusteeService(mockErsUtil, mockSessionService)

    val trusteeOne   = TrusteeDetails("First Trustee", "20 Garden View", None, None, None, None, None, true)
    val trusteeTwo   = TrusteeDetails("Second Trustee", "72 Big Avenue", None, None, None, None, None, true)
    val trusteeThree = TrusteeDetails("Third Trustee", "21 Brick Lane", None, None, None, None, None, true)
    val trusteeFour  = TrusteeDetails("Fourth Trustee", "21 Brick Lane", None, None, None, None, None, true)
    val replacement  = TrusteeDetails("Replacement Trustee", "1 Some Place", None, None, None, None, None, true)

    val trusteeDetailsList = List(
      trusteeOne,
      trusteeTwo,
      trusteeThree,
      trusteeFour
    )

    "add a new trustee to the list" when {
      "the index is 10000" in {

        val index = 10000

        val expectedOutput = List(
          trusteeOne,
          trusteeTwo,
          trusteeThree,
          trusteeFour,
          replacement
        )

        val result = trusteeService.replaceTrustee(trusteeDetailsList, index, replacement)

        result shouldBe expectedOutput
      }
    }

    "replace a trustee and keep the other trustees" when {

      "given an index that matches a trustee in the list" in {

        val index = 2

        val expectedOutput = List(
          trusteeOne,
          trusteeTwo,
          replacement,
          trusteeFour
        )

        val result = trusteeService.replaceTrustee(trusteeDetailsList, index, replacement)

        result shouldBe expectedOutput
      }
    }

    "keep the existing list of trustees" when {

      "given an index that does not match any existing trustees" in {

        val index = 100

        val result = trusteeService.replaceTrustee(trusteeDetailsList, index, replacement)

        result shouldBe trusteeDetailsList
      }
    }

    "remove duplicate records" when {

      "duplicates are present" in {

        val index = 1

        val target = new TrusteeDetails("Target Company", "3 Window Close", None, None, None, None, None, true)

        val duplicateTrusteeDetailsList = List(
          trusteeOne,
          target,
          target,
          target
        )

        val expectedOutput = List(
          trusteeOne,
          target
        )

        val result = trusteeService.replaceTrustee(duplicateTrusteeDetailsList, index, target)

        result shouldBe expectedOutput
      }
    }
  }

  "deleteTrustee" must {
    val trusteeService: TrusteeService = new TrusteeService(mockErsUtil, mockSessionService)

    when(mockSessionService.fetch[RequestObject](any())(any(), any())).thenReturn(Future.successful(ersRequestObject))
    when(mockSessionService.fetch[TrusteeDetailsList](any())(any(), any())).thenReturn(Future.successful(exampleTrustees))

    "return true" when {
      "delete operation was successful" in {
        when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.successful(sessionPair))

        val result = trusteeService.deleteTrustee(0).futureValue

        result shouldBe true
      }
    }

    "return false" when {
      "delete operation failed" in {
        when(mockSessionService.cache(any(), any())(any(), any())).thenReturn(Future.failed(new Exception("caching failed")))

        val result = trusteeService.deleteTrustee(0).futureValue

        result shouldBe false
      }
    }
  }
}
