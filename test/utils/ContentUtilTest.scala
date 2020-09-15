/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

class ContentUtilTest extends UnitSpec with ERSFakeApplicationConfig with MockitoSugar with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer

  "getSchemeName" should {
    "return CSOP scheme name for CSOP scheme type \"1\"" in {
      val schemeType = "1"
      val result = ContentUtil.getSchemeName(schemeType)
      result shouldBe Messages("ers_pdf_error_report.csop")
    }
  }

  "getSchemeName" should {
    "return EMI scheme name for EMI scheme type \"2\"" in {
      val schemeType = "2"
      val result = ContentUtil.getSchemeName(schemeType)
      result shouldBe Messages("ers_pdf_error_report.emi")
    }
  }
  "getSchemeName" should {
    "return SAYE scheme name for SAYE scheme type \"4\"" in {
      val schemeType = "4"
      val result = ContentUtil.getSchemeName(schemeType)
      result shouldBe Messages("ers_pdf_error_report.saye")
    }
  }
  "getSchemeName" should {
    "return SIP scheme name for SIP scheme type \"5\"" in {
      val schemeType = "5"
      val result = ContentUtil.getSchemeName(schemeType)
      result shouldBe Messages("ers_pdf_error_report.sip")
    }
  }
  "getSchemeName" should {
    "return OTHER scheme name for OTHER scheme type \"3\"" in {
      val schemeType = "3"
      val result = ContentUtil.getSchemeName(schemeType)
      result shouldBe Messages("ers_pdf_error_report.other")
    }
  }
  "getSchemeName" should {
    "return \"\" scheme name for any other scheme type e.g. \"9\"" in {
      val schemeType = "9"
      val result = ContentUtil.getSchemeName(schemeType)
      result shouldBe ""
    }
  }

  "getSchemeAbbreviation" should {
    "return CSOP scheme abbreviation for CSOP scheme type \"1\"" in {
      val schemeType = "1"
      val result = ContentUtil.getSchemeAbbreviation(schemeType)
      result shouldBe Messages("ers.csop")
    }
  }

  "getSchemeAbbreviation" should {
    "return EMI scheme abbreviation for EMI scheme type \"2\"" in {
      val schemeType = "2"
      val result = ContentUtil.getSchemeAbbreviation(schemeType)
      result shouldBe Messages("ers.emi")
    }
  }
  "getSchemeAbbreviation" should {
    "return SAYE scheme abbreviation for SAYE scheme type \"4\"" in {
      val schemeType = "4"
      val result = ContentUtil.getSchemeAbbreviation(schemeType)
      result shouldBe Messages("ers.saye")
    }
  }
  "getSchemeAbbreviation" should {
    "return SIP scheme abbreviation for SIP scheme type \"5\"" in {
      val schemeType = "5"
      val result = ContentUtil.getSchemeAbbreviation(schemeType)
      result shouldBe Messages("ers.sip")
    }
  }
  "getSchemeAbbreviation" should {
    "return OTHER scheme abbreviation for OTHER scheme type \"3\"" in {
      val schemeType = "3"
      val result = ContentUtil.getSchemeAbbreviation(schemeType)
      result shouldBe Messages("ers.other")
    }
  }
  "getSchemeAbbreviation" should {
    "return \"\" scheme abbreviation for any other scheme type e.g. \"9\"" in {
      val schemeType = "9"
      val result = ContentUtil.getSchemeAbbreviation(schemeType)
      result shouldBe ""
    }
  }

  for (scheme <- List("csop", "emi", "saye", "sip", "other")) {
    "getSchemeAbbreviation" should {
      s"return scheme abbreviation given $scheme" in {
        val result = ContentUtil.getSchemeAbbreviation(scheme)
        result shouldBe Messages(s"ers.$scheme")
      }
    }
  }

}
