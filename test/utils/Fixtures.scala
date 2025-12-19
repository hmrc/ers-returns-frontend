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

package utils

import models._
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator

import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

object Fixtures extends AuthHelper {
  val firstName    = "FirstName"
  val middleName   = "MiddleName"
  val surname      = "Surname"
  val nino: String = new Generator().nextNino.nino
  val companyName  = "Company Name"

  def getAwaitDuration: Duration = 60.seconds

  val buildFakeUser: ERSAuthData = defaultErsAuthData

  def buildFakeRequestWithSessionId(method: String): FakeRequest[AnyContentAsEmpty.type]     =
    FakeRequest()
      .withSession(("sessionId" -> "FAKE_SESSION_ID"), ("screenSchemeInfo" -> "2 - EMI - MYScheme - XX12345678 - 2016"))

	def buildFakeRequestWithSessionIdCSOP(method: String): FakeRequest[AnyContentAsEmpty.type] = {
	 FakeRequest().withSession(("sessionId" -> "FAKE_SESSION_ID"), ("screenSchemeInfo" -> "1 - CSOP - MYScheme - XX12345678 - 2016"))
	}

  def buildFakeRequestWithSessionIdSAYE(method: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      ("sessionId"        -> "FAKE_SESSION_ID"),
      ("screenSchemeInfo" -> "4 - SAYE - MYScheme - XX12345678 - 2016")
    )

  def buildFakeRequestWithSessionIdSIP(method: String): FakeRequest[AnyContentAsEmpty.type]   =
    FakeRequest()
      .withSession(("sessionId" -> "FAKE_SESSION_ID"), ("screenSchemeInfo" -> "5 - SIP - MYScheme - XX12345678 - 2016"))

  def buildFakeRequestWithSessionIdEMI(method: String): FakeRequest[AnyContentAsEmpty.type]   =
    FakeRequest()
      .withSession(("sessionId" -> "FAKE_SESSION_ID"), ("screenSchemeInfo" -> "2 - EMI - MYScheme - XX12345678 - 2016"))

  def buildFakeRequestWithSessionIdOTHER(method: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      ("sessionId"        -> "FAKE_SESSION_ID"),
      ("screenSchemeInfo" -> "3 - OTHER - MYScheme - XX12345678 - 2016")
    )

  def buildFakeRequest(method: String) = FakeRequest()

  def schemeRef: String = "XYZ12345"

  val timestamp: ZonedDateTime = ZonedDateTime.now

  val schemeType = "EMI"

  val EMISchemeInfo: SchemeInfo = SchemeInfo(
    schemeRef = "XA1100000000000",
    timestamp = Instant.now,
    schemeId = "123AA12345678",
    taxYear = "2014/15",
    schemeName = "My scheme",
    schemeType = schemeType
  )

  val EMIMetaData = ErsMetaData(
    schemeInfo = EMISchemeInfo,
    ipRef = "127.0.0.0",
    aoRef = Some("123AA12345678"),
    empRef = "EMI - MyScheme - XA1100000000000 - 2014/15",
    agentRef = None,
    sapNumber = Some("sap-123456")
  )

  val scheetName: String = "EMI40_Adjustments_V4"
  val data: Option[ListBuffer[Seq[String]]] = Some(
    ListBuffer(
      Seq(
        "no",
        "no",
        "yes",
        "3",
        "2015-12-09",
        firstName,
        "",
        surname,
        nino,
        "123/XZ55555555",
        "10.1234",
        "100.12",
        "10.1234",
        "10.1234"
      ),
      Seq(
        "no",
        "no",
        "no",
        "",
        "2015-12-09",
        firstName,
        "",
        surname,
        nino,
        "123/XZ55555555",
        "10.1234",
        "100.12",
        "10.1234",
        "10.1234"
      ),
      Seq(
        "yes",
        "",
        "",
        "",
        "2015-12-09",
        firstName,
        middleName,
        surname,
        nino,
        "123/XZ55555555",
        "10.1234",
        "100.12",
        "10.1234",
        "10.1234"
      )
    )
  )

  val invalidJson: JsObject = Json.obj(
    "metafield1" -> "metavalue1",
    "metafield2" -> "metavalue2",
    "metafield3" -> "metavalue3"
  )

  val schemeOrganiserDetails: SchemeOrganiserDetails = SchemeOrganiserDetails(
    "companyName",
    "addressLine1",
    None,
    None,
    None,
    None,
    None,
    None,
    Some("corporationRef")
  )

  val companyDetails: CompanyDetails = CompanyDetails(
    "testCompany",
    "testAddress1",
    Some("testAddress2"),
    Some("testAddress3"),
    Some("testAddress4"),
    Some("AA1 1AA"),
    Some("United Kingdom"),
    Some("1234567890"),
    Some("1234567890"),
    true
  )

  val groupScheme = GroupSchemeInfo(Some("1"), Some("emi"))

  val companiesList = CompanyDetailsList(List(companyDetails))
  val ersSummary    = ErsSummary(
    "testbundle",
    "1",
    None,
    ZonedDateTime.of(LocalDateTime.of(2016, 6, 8, 11, 5), ZoneId.systemDefault).toInstant,
    metaData = EMIMetaData,
    None,
    None,
    Some(groupScheme),
    Some(schemeOrganiserDetails),
    Some(companiesList),
    None,
    None,
    None
  )

  val metadataJson: JsObject = Json.toJson(EMIMetaData).as[JsObject]

  val ersRequestObject =
    RequestObject(
      Some("123AA12345678"),
      Some("2014/15"),
      Some("AA0000000000000"),
      Some("Other"),
      Some("OTHER"),
      Some("agentRef"),
      Some("empRef"),
      Some("ts"),
      Some("hmac")
    )

  val companyAddressOverseas: CompanyAddress = CompanyAddress(
    "Overseas 1",
    Some("2"),
    Some("3"),
    Some("4"),
    Some("5"),
    Some("country")
  )

  val companyAddressUK: CompanyAddress = CompanyAddress(
    "UK 1",
    Some("2"),
    Some("3"),
    Some("4"),
    Some("5"),
    Some("UK")
  )

  val companyUKDetails: Company = Company(
    "FunnyCompany",
    Some("AA123456"),
    Some("1234567890")
  )

  val trusteeAddressOverseas: TrusteeAddress = TrusteeAddress(
    "Overseas line 1",
    Some("Overseas line 2"),
    Some("Overseas line 3"),
    Some("Overseas line 4"),
    Some("Overseas line 5"),
    Some("Overseas country")
  )

  val trusteeAddressUk: TrusteeAddress = TrusteeAddress(
    "UK line 1",
    Some("UK line 2"),
    Some("UK line 3"),
    Some("UK line 4"),
    Some("UK line 5"),
    Some("UK")
  )

  val exampleTrustees: TrusteeDetailsList = TrusteeDetailsList(List(
    TrusteeDetails(TrusteeName("John Bonson"), trusteeAddressUk),
    TrusteeDetails(TrusteeName("Dave Daveson"), trusteeAddressOverseas)
  ))

  val exampleCompanies: CompanyDetailsList = CompanyDetailsList(List(
    CompanyDetails(Company("Company1", Some("AA123456"), Some("1234567890")), companyAddressUK),
    CompanyDetails(Company("Company2", Some("BB123456"), Some("0987654321")), companyAddressOverseas)
  ))

  val exampleSchemeOrganiserUk: CompanyDetails = CompanyDetails(Company("Company1", Some("AA123456"), Some("1234567890")), companyAddressUK)

  val exampleSchemeOrganiserOverseas: CompanyDetails =  CompanyDetails(Company("Company2", Some("BB123456"), Some("0987654321")), companyAddressOverseas)


}
