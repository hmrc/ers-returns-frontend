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

package config

import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.DurationInt

class ApplicationConfigSpec extends AnyWordSpecLike with GuiceOneAppPerSuite {
  private val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  private val baseConfig: Configuration    = app.injector.instanceOf[Configuration]

  "ApplicationConfig" when {

    "using the default application config" should {
      "return the correct values" in {
        appConfig.appName                       mustBe "ers-returns-frontend"
        appConfig.ersUrl                        mustBe "http://localhost:9291"
        appConfig.validatorUrl                  mustBe "http://localhost:9226"
        appConfig.urBannerLink                  mustBe "https://signup.take-part-in-research.service.gov.uk/?utm_campaign=ERS_confirmation&utm_source=Survey_Banner&utm_medium=other&t=HMRC&id=130"
        appConfig.ggSignInUrl                   mustBe "http://localhost:9949/auth-login-stub/gg-sign-in/"
        appConfig.csopV5Enabled                 mustBe false
        appConfig.odsSuccessRetryAmount         mustBe 10
        appConfig.odsValidationRetryAmount      mustBe 10
        appConfig.retryDelay                    mustBe 2000.milliseconds
        appConfig.sentViaSchedulerNoOfRowsLimit mustBe 10000
        appConfig.enableRetrieveSubmissionData  mustBe true
        appConfig.allCsvFilesCacheRetryAmount   mustBe 10
        appConfig.accessThreshold               mustBe 100

      }
    }

    "fall back to the default values for useV4andV5Scheme and useV6andV7Scheme when values are missing" in {
      val stripped = Configuration(
        baseConfig.underlying
          .withoutPath("microservice.services.features.scheme-version.use-V4-and-V5")
          .withoutPath("microservice.services.features.scheme-version.use-V6-and-V7")
      )
      val config = new ApplicationConfig(new ServicesConfig(stripped))
      config.useV4andV5Scheme mustBe true
      config.useV6andV7Scheme mustBe false

    }
  }

}
