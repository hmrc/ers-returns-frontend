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

package utils

import akka.stream.Materializer
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Environment
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, DefaultActionBuilder, DefaultMessagesControllerComponents, MessagesControllerComponents}
import play.api.test.Helpers.stubBodyParser

import java.io.InputStream
import scala.concurrent.ExecutionContext

class CountryCodesSpec extends AnyWordSpecLike
  with Matchers
  with OptionValues
  with ERSFakeApplicationConfig
  with ErsTestHelper
  with GuiceOneAppPerSuite {

  val mockMCC: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  lazy val appEnvironment: Environment = app.injector.instanceOf[Environment]
	implicit lazy val mat: Materializer = app.materializer

  object TestCountryCodes extends CountryCodes {
		override def environment: Environment = appEnvironment
    override val jsonInputStream: Option[InputStream] = appEnvironment.resourceAsStream("country-code-test.json")
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
