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

import java.io.InputStream

import akka.stream.Materializer
import org.scalatestplus.play.OneServerPerSuite
import play.api.Play.current
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Play}
import uk.gov.hmrc.play.test.UnitSpec

class CountryCodesSpec extends UnitSpec with OneServerPerSuite with ERSFakeApplicationConfig {

  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
	lazy val appEnvironment: Environment = app.injector.instanceOf[Environment]
	implicit lazy val mat: Materializer = app.materializer

  object TestCountryCodes extends CountryCodes {
		override def environment: Environment = appEnvironment
    override val jsonInputStream: Option[InputStream] = Play.application.resourceAsStream("country-code-test.json")
  }

	trait Setup {
		val countryCodes = new CountryCodesImpl(appEnvironment)
	}

  "CountryCode countries" should {
    "return a string of countries" in new Setup {
			val countries: String = countryCodes.countries
      countries should include("Andorra")
      countries should include("Germany")
      countries should include("France")
    }

    "not return a string of countries" in {
      intercept[Exception] {
        TestCountryCodes.countries
      }
    }
  }

  "CountryCode getCountry" should {
    "return a country from a country code" in new Setup {
      countryCodes.getCountry("AD") should be(Some("Andorra"))
      countryCodes.getCountry("DE") should be(Some("Germany"))
      countryCodes.getCountry("FR") should be(Some("France"))
      countryCodes.getCountry("ZZ") should be(None)
    }
  }

}
