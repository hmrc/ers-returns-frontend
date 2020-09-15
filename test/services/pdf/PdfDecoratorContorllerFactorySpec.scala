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

package services.pdf

import akka.stream.Materializer
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CountryCodes, ERSFakeApplicationConfig, Fixtures}

class PdfDecoratorContorllerFactorySpec extends UnitSpec with OneAppPerSuite with ERSFakeApplicationConfig with MockitoSugar {
  override lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  implicit lazy val mat: Materializer = app.materializer

	class TestPdfDecoratorControllerFactory extends PdfDecoratorControllerFactory {
		val mockCountryCodes: CountryCodes = mock[CountryCodes]
		override val countryCodes: CountryCodes = mockCountryCodes
	}

  // a function to get matching an instance to be of certain type
  def anInstanceOf[T](implicit manifest: Manifest[T]): BePropertyMatcher[AnyRef] = {
    val clazz = manifest.runtimeClass.asInstanceOf[Class[T]]
    new BePropertyMatcher[AnyRef] {
      def apply(left: AnyRef): BePropertyMatchResult =
        BePropertyMatchResult(clazz.isAssignableFrom(left.getClass), "an instance of " + clazz.getName)
    }
  }

  "extended pdf scheme decortator factory" should {
    "create new emi scheme decorator when scheme is EMI" in new TestPdfDecoratorControllerFactory {
      val decorator: DecoratorController = createPdfDecoratorControllerForScheme("emi", Fixtures.ersSummary, None)
      decorator should be(anInstanceOf[DecoratorController])
    }

    "throw invalid argument exception if scheme is not supported" in new TestPdfDecoratorControllerFactory {
      intercept[IllegalArgumentException] {
        createPdfDecoratorControllerForScheme("blah", Fixtures.ersSummary, None)
      }
    }
  }
}
