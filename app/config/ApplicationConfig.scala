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

package config

import controllers.routes
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Mode.Mode
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.duration._

@Singleton
class ApplicationConfig @Inject()(val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {

	lazy val languageMap: Map[String, Lang] = Map(
		"english" -> Lang("en"),
		"cymraeg" -> Lang("cy")
	)

	def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)
	override protected def mode: Mode = environment.mode

	lazy val appName: String = getString("appName")
	lazy val authBaseUrl: String = baseUrl("auth")
	lazy val googleTagManagerId: String = getString("google-tag-manager.id")

	lazy val contactFormServiceIdentifier = "ers-returns"
	lazy val contactHost: String = getString("contact-frontend.host")

	lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
	lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
	lazy val gaToken: String = getString(s"govuk-tax.google-analytics.token")

	lazy val assetsPrefix: String = getString("assets.url") + getString("assets.version")
	lazy val analyticsToken: String = getString("govuk-tax.google-analytics.token")
	lazy val analyticsHost: String = getString("govuk-tax.google-analytics.host")
	lazy val ersUrl: String = baseUrl("ers-returns")
	lazy val validatorUrl: String = baseUrl("ers-file-validator")

	lazy val upscanProtocol: String = getConfString("upscan.protocol","http").toLowerCase()
	lazy val upscanInitiateHost: String = baseUrl("upscan")
	lazy val upscanRedirectBase: String = getString("microservice.services.upscan.redirect-base")

	lazy val urBannerToggle: Boolean = getBoolean("urBanner.toggle")
	lazy val urBannerLink: String = getString("urBanner.link")
	lazy val ggSignInUrl: String = getString("government-gateway-sign-in.host")

	lazy val enableRetrieveSubmissionData: Boolean = getBoolean("settings.enable-retrieve-submission-data")
	lazy val languageTranslationEnabled: Boolean = getConfBool("features.welsh-translation", defBool = true)

	lazy val odsSuccessRetryAmount: Int = getInt("retry.ods-success-cache.complete-upload.amount")
	lazy val odsValidationRetryAmount: Int = getInt("retry.ods-success-cache.validation.amount")
	lazy val allCsvFilesCacheRetryAmount: Int = getInt("retry.csv-success-cache.all-files-complete.amount")
	lazy val retryDelay: FiniteDuration = runModeConfiguration.getMilliseconds("retry.delay").get milliseconds
	lazy val accessThreshold: Int = getInt("accessThreshold")

	lazy val sentViaSchedulerNoOfRowsLimit: Int = 10000

	//Previous ExternalUrls Object
	lazy val companyAuthHost: String = getString(s"$rootServices.auth.company-auth.host")
	lazy val signOutCallback: String = getString(s"$rootServices.feedback-survey-frontend.url")
	lazy val signOut = s"$companyAuthHost/gg/sign-out?continue=$signOutCallback"
	lazy val loginCallback: String = getString(s"$rootServices.auth.login-callback.url")
	lazy val portalDomain: String = getString("portal.domain")
	lazy val hmacToken: String = getString("hmac.hmac_token")
	lazy val hmacOnSwitch: Boolean = getBoolean("hmac.hmac_switch")

	//SessionCacheWiring
	lazy val shortLivedCacheBaseUri: String = baseUrl("cachable.short-lived-cache")
	lazy val shortLivedCacheDomain: String = getString(s"$rootServices.cachable.short-lived-cache.domain")
	lazy val sessionCacheBaseUri: String = baseUrl("cachable.session-cache")
	lazy val sessionCacheDomain: String = getString(s"$rootServices.cachable.session-cache.domain")

}
