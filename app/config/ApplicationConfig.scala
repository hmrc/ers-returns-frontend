/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig
import play.Logger
import scala.util.Try

trait ApplicationConfig {

  val assetsPrefix: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val uploadCollection: String
  val validatorUrl: String

  val platformHostUrl: String
  val successPageUrl: String
  val failurePageUrl: String
  val callbackPageUrl: String

  val successCsvPageUrl: String
  val failureCsvPageUrl: String
  val callbackCsvPageUrl: String
  val enableRetrieveSubmissionData: Boolean
  val sentViaSchedulerNoOfRowsLimit: Int

  val ggSignInUrl: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  Logger.info("The Getting the contact host")
  private val contactHost = configuration.getString(s"$env.microservice.services.contact-frontend.host").getOrElse("")
  Logger.info("The contact host is " + contactHost)
  private val contactFormServiceIdentifier = "ERS"

  override lazy val assetsPrefix: String = loadConfig(s"$env.assets.url") + loadConfig(s"$env.assets.version")
  override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"
  override lazy val analyticsToken: Option[String] = configuration.getString(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"govuk-tax.$env.google-analytics.host").getOrElse("service.gov.uk")
  override lazy val uploadCollection: String = loadConfig(s"$env.settings.upload-collection")

  override lazy val validatorUrl: String = baseUrl("ers-file-validator") + "/ers/:empRef/" + loadConfig(s"$env.microservice.services.ers-file-validator.url")

  private val frontendHost = loadConfig(s"$env.platform.frontend.host")
  override lazy val platformHostUrl = Try{baseUrl("ers-returns-frontend")}.getOrElse("")
  override lazy val successPageUrl: String = frontendHost + loadConfig(s"$env.microservice.services.ers-returns-frontend.success-page")
  override lazy val failurePageUrl: String = frontendHost + loadConfig(s"$env.microservice.services.ers-returns-frontend.failure-page")
  override lazy val callbackPageUrl: String = platformHostUrl + loadConfig(s"$env.microservice.services.ers-returns-frontend.callback-page")
  override lazy val successCsvPageUrl: String = frontendHost + loadConfig(s"$env.microservice.services.ers-returns-frontend.csv-success-page")
  override lazy val failureCsvPageUrl: String = frontendHost + loadConfig(s"$env.microservice.services.ers-returns-frontend.csv-failure-page")
  override lazy val callbackCsvPageUrl: String = platformHostUrl + loadConfig(s"$env.microservice.services.ers-returns-frontend.csv-callback-page")

  override val ggSignInUrl: String = configuration.getString(s"$env.government-gateway-sign-in.host").getOrElse("")

  override lazy val enableRetrieveSubmissionData: Boolean = Try(loadConfig(s"$env.settings.enable-retrieve-submission-data").toBoolean).getOrElse(false)
  override lazy val sentViaSchedulerNoOfRowsLimit: Int = {
    Logger.info("sent-via-scheduler-noofrows vakue is " + Try(loadConfig("sent-via-scheduler-noofrows").toInt).getOrElse(10000))
    Try(loadConfig("sent-via-scheduler-noofrows").toInt).getOrElse(10000)
  }

}
