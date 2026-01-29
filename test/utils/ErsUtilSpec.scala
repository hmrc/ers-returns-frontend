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

package utils

import models._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

class ErsUtilSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach
    with ERSFakeApplicationConfig
    with ErsTestHelper
    with ScalaFutures {

  implicit override val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))
  implicit val countryCodes: CountryCodes = mockCountryCodes

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionService)
  }

  "calling buildAddressSummary" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "build an address summary from CompanyDetails" in {
      val companyDetails = CompanyDetails(
        "name",
        addressLine1 = "ADDRESS1",
        addressLine2 = Some("ADDRESS2"),
        addressLine3 = None,
        addressLine4 = None,
        addressLine5 = Some("AB123CD"),
        country = Some("UK"),
        companyReg = Some("ABC"),
        corporationRef = Some("DEF"),
        basedInUk = true
      )
      val expected       = "ADDRESS1, ADDRESS2, AB123CD, United Kingdom"
      val addressSummary = ersUtil.buildAddressSummary(companyDetails)
      assert(addressSummary == expected)
    }

    "build an address summary from TrusteeDetails" in {
      val companyDetails = TrusteeDetails(
        name = "NAME",
        addressLine1 = "ADDRESS1",
        addressLine2 = Some("ADDRESS2"),
        addressLine3 = None,
        addressLine4 = None,
        country = Some("UK"),
        addressLine5 = Some("AB123CD"),
        basedInUk = true
      )
      val expected       = "ADDRESS1, ADDRESS2, AB123CD, United Kingdom"
      val addressSummary = ersUtil.buildAddressSummary(companyDetails)
      assert(addressSummary == expected)
    }

    "build an empty String for anything else" in {
      val expected = ""
      assert(ersUtil.buildAddressSummary(null) == expected)
      assert(ersUtil.buildAddressSummary("Hello") == expected)
      assert(ersUtil.buildAddressSummary(3.14) == expected)
    }
  }

  "calling replaceAmpersand" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "do nothing to a string with no ampersands" in {
      val input = "I am some test input"
      ersUtil.replaceAmpersand(input) shouldBe "I am some test input"
    }

    "replace any ampersands with &amp;" in {
      val input = "I am some test input & stuff &"
      ersUtil.replaceAmpersand(input) shouldBe "I am some test input &amp; stuff &amp;"
    }

    "not affect any &amp; that already exists" in {
      val input = "I am some test input & stuff &amp;"
      ersUtil.replaceAmpersand(input) shouldBe "I am some test input &amp; stuff &amp;"
    }
  }

  "concatEntity" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "concatenate all defined strings with existing entity lines" in {
      val optionalLines = List(Some("Line2"), Some("Line3"))
      val existingLines = "Company, Line1"
      ersUtil.concatEntity(optionalLines, existingLines) shouldBe "Company, Line1, Line2, Line3"
    }

    "handle Some and None values correctly" in {
      val optionalLines = List(Some("Line2"), None, Some("Line3"))
      val existingLines = "Company, Line1"
      ersUtil.concatEntity(optionalLines, existingLines) shouldBe "Company, Line1, Line2, Line3"
    }

    "return existing lines only when all optional lines are None" in {
      val optionalLines = List(None, None)
      val existingLines = "Company, Line1"
      ersUtil.concatEntity(optionalLines, existingLines) shouldBe "Company, Line1"
    }
  }

  "buildEntitySummary" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "build summary with all fields present" in {
      val entity = SchemeOrganiserDetails(
        "Company",
        "Line1",
        Some("Line2"),
        Some("Line3"),
        Some("Line4"),
        Some("Country"),
        Some("Postcode"),
        Some("Reg"),
        Some("Ref")
      )
      ersUtil.buildEntitySummary(entity) shouldBe "Company, Line1, Line2, Line3, Line4, Country, Postcode, Reg, Ref"
    }

    "handle missing optional fields" in {
      val entity =
        SchemeOrganiserDetails("Company", "Line1", None, Some("Line3"), None, Some("Country"), None, None, None)
      ersUtil.buildEntitySummary(entity) shouldBe "Company, Line1, Line3, Country"
    }
  }

  "buildCompanyNameList" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "handle an empty list" in {
      ersUtil.buildCompanyNameList(List.empty) shouldBe ""
    }

    "handle a non-empty list" in {
      val companies = List(
        CompanyDetails(
          companyName = "Company1",
          addressLine1 = "",
          None,
          None,
          None,
          None,
          country = Some("UK"),
          None,
          None,
          true
        ),
        CompanyDetails(
          companyName = "Company2",
          addressLine1 = "",
          None,
          None,
          None,
          None,
          country = Some("UK"),
          None,
          None,
          true
        )
      )
      ersUtil.buildCompanyNameList(companies) shouldBe "Company1<br>Company2<br>"
    }
  }

  "buildTrusteeNameList" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "handle an empty list" in {
      ersUtil.buildTrusteeNameList(List.empty) shouldBe ""
    }

    "handle a non-empty list" in {
      val trustees = List(
        TrusteeDetails("Trustee1", "1 The Street", None, None, None, Some("UK"), None, true),
        TrusteeDetails("Trustee2", "1 The Street", None, None, None, Some("UK"), None, true)
      )
      ersUtil.buildTrusteeNameList(trustees) shouldBe "Trustee1<br>Trustee2<br>"
    }
  }

  "companyLocation" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "return OVERSEAS for non-default country" in {
      ersUtil.companyLocation(
        CompanyDetails(
          companyName = "",
          addressLine1 = "",
          None,
          None,
          None,
          None,
          country = Some("FR"),
          None,
          None,
          false
        )
      ) shouldBe "ers_trustee_based.overseas"
    }

    "return default country name" in {
      ersUtil.companyLocation(
        CompanyDetails(
          companyName = "",
          addressLine1 = "",
          None,
          None,
          None,
          None,
          country = Some("UK"),
          None,
          None,
          true
        )
      ) shouldBe "ers_trustee_based.uk"
    }
  }

  "trusteeLocationMessage" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "return ers_trustee_based.uk for UK-based trustee" in {
      ersUtil.trusteeLocationMessage(
        TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("UK"), None, true)
      ) shouldBe "ers_trustee_based.uk"
    }

    "return ers_trustee_based.overseas for overseas-based trustee" in {
      ersUtil.trusteeLocationMessage(
        TrusteeDetails("First Trustee", "1 The Street", None, None, None, Some("FR"), None, false)
      ) shouldBe "ers_trustee_based.overseas"
    }
  }

  "addCompanyMessage" should {
    val ersUtil: ERSUtil = new ERSUtil(mockAppConfig)

    "return appropriate message for Some scheme option" in {
      val messages = mock[Messages]
      when(messages.apply("ers_group_summary.csop.add_company")).thenReturn("Add CSOP company")

      ersUtil.addCompanyMessage(messages, Some("CSOP")) shouldBe "Add CSOP company"
    }

    "return appropriate message for None scheme option" in {
      val messages = mock[Messages]
      when(messages.apply("ers_group_summary..add_company")).thenReturn("Add company")

      ersUtil.addCompanyMessage(messages, None) shouldBe "Add company"
    }
  }

}
