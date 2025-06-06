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

package models

import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.DateUtils
import utils.Fixtures.ersRequestObject

import java.time.ZonedDateTime

class RequestObjectSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite with PrivateMethodTester {

  def messagesApi: MessagesApi    = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))

  val englishMessages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))
  val welshMessages: Messages = messagesApi.preferred(Seq(Lang.get("cy").get))

  "RequestObject" should {

    "return a page title with the correct format" in {

      val expected =
        s"${messages(s"ers.scheme.${ersRequestObject.getSchemeType}")} - ${messages(s"ers.scheme.title", "Other")} - ${ersRequestObject.getSchemeReference} - ${DateUtils
          .getFullTaxYear(ersRequestObject.getTaxYear)}"

      ersRequestObject.getPageTitle mustBe expected
    }

    "return a page title in Welsh with the correct format for scheme Other" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("cy").get))

      val expected =
        s"${messages(s"ers.scheme.${ersRequestObject.getSchemeType}")} - ${messages(s"ers.scheme.title", "Arall")} - ${ersRequestObject.getSchemeReference} - ${DateUtils
          .getFullTaxYear(ersRequestObject.getTaxYear)}"

      ersRequestObject.getPageTitle mustBe expected
    }

    "return start page title in Welsh with the correct translation for scheme Other" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("cy").get))

      messages("ers_start.page_title", ersRequestObject.getSchemeNameForDisplay) must include("Arall")
    }

    "return the correct scheme id" in {

      val expected = "3"

      ersRequestObject.getSchemeId mustBe expected
    }

    "return an instance of SchemeInfo with the correct field" in {

      val requestObject =
        RequestObject(
          None,
          Some("2016/17"),
          Some("AA0000000000000"),
          Some("MyScheme"),
          Some("CSOP"),
          None,
          None,
          None,
          None
        )
      val privateToSchemeInfo = PrivateMethod[SchemeInfo](Symbol("toSchemeInfo"))
      val result = requestObject invokePrivate privateToSchemeInfo()

      result.schemeName mustBe "MyScheme"
      result.schemeId mustBe "1"
      result.schemeType mustBe "CSOP"
      result.schemeRef mustBe "AA0000000000000"
      result.taxYear mustBe "2016/17"
    }

    "return an instance of ErsMetaData with the correct field" in {

      implicit val request = FakeRequest("GET", "/foo")

      val requestObject =
        RequestObject(
          None,
          Some("2016/17"),
          Some("AA0000000000000"),
          Some("MyScheme"),
          Some("CSOP"),
          None,
          Some("empRef"),
          None,
          None
        )

      val expectedSchemeInfo =
        SchemeInfo(
          "AA0000000000000",
          null, // absolute DateTime comparison is brittle, tolerance checked below
          "1",
          "2016/17",
          "MyScheme",
          "CSOP"
        )

      val result             = requestObject.toErsMetaData
      val resultTimestamp    = result.schemeInfo.timestamp
      val adjustedSchemeInfo = result.schemeInfo.copy(timestamp = null)
      val diff: Int = resultTimestamp.compareTo(ZonedDateTime.now)

      diff must be < 100
      adjustedSchemeInfo mustBe expectedSchemeInfo
      result.ipRef mustBe request.remoteAddress
      result.aoRef mustBe None
      result.empRef mustBe "empRef"
      result.agentRef mustBe None
      result.sapNumber mustBe None
    }
  }

  "RequestObject.startsWithVowel" should {
    "return true if the scheme starts with a vowel" in {
      RequestObject.startsWithVowel("EMI") mustBe true
      RequestObject.startsWithVowel("OTHER") mustBe true
    }

    "return false is the scheme starts with a consonant" in {
      RequestObject.startsWithVowel("CSOP") mustBe false
      RequestObject.startsWithVowel("SIP") mustBe false
    }

    "return false is the scheme is empty" in {
      RequestObject.startsWithVowel("") mustBe false
    }
  }

  "RequestObject.getSchemeWithArticle" should {
    "prepend 'an' for schemes beginning with vowel when lang code is 'en'" in {
      implicit val messages: Messages = englishMessages
      val result = RequestObject.getSchemeWithArticle("EMI")
      result mustBe "an EMI"
    }

    "prepend 'a' for schemes beginning with consonant when lang code is 'en'" in {
      implicit val messages: Messages = englishMessages
      val result = RequestObject.getSchemeWithArticle("CSOP")
      result mustBe "a CSOP"
    }

    "return 'ARALL' for scheme = 'OTHER' when lang code is 'cy'" in {
      implicit val messages: Messages = welshMessages
      val result = RequestObject.getSchemeWithArticle("OTHER")
      result mustBe "ARALL"
    }

    "default to 'a <scheme>' for unknown lang codes" in {
      val frenchMessages: Messages = messagesApi.preferred(Seq(Lang.get("fr").get))
      implicit val messages: Messages = frenchMessages
      val result = RequestObject.getSchemeWithArticle("CSOP")
      result mustBe "a CSOP"
    }
  }
}
