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

trait ERSFakeApplicationConfig {
  val config: Map[String, Any] = Map("play.crypto.secret" -> "test",
    "govuk-tax.Test.login-callback.url" -> "test",
    "govuk-tax.Test.assets.url" -> "test",
    "govuk-tax.Test.assets.version" -> "version",
    "Test.external-url.contact-frontend.host" -> "test",
    "Test.external-url.contact-frontend.host" -> "test",
    "Test.external-url.tai-frontend.host" -> "test",
    "govuk-tax.Test.login-callback.url" -> "test",
    "govuk-tax.Test.services.contact-frontend.host" -> "test",
    "govuk-tax.Test.services.contact-frontend.port" -> "test",
    "metrics.enabled" -> "true",
    "auditing.enabled" -> false,
    "microservice.metrics.graphite.enabled" -> false,
    "microservice.services.upscan.redirect-base" -> "http://localhost:9000"
  )
}
