/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.routes
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton
import scala.concurrent.duration._

@Singleton
class ApplicationConfig @Inject()(config: ServicesConfig) {

	lazy val languageMap: Map[String, Lang] = Map(
		"english" -> Lang("en"),
		"cymraeg" -> Lang("cy")
	)

	def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

	lazy val csp: String = config.getString("play.filters.csp.directives.script-src")

	lazy val appName: String = config.getString("appName")
	lazy val authBaseUrl: String = config.baseUrl("auth")
	lazy val googleTagManagerId: String = config.getString("google-tag-manager.id")

	lazy val contactFormServiceIdentifier = "ers-returns"
	lazy val contactHost: String = config.getString("contact-frontend.host")

	lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
	lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

	lazy val assetsPrefix: String = config.getString("assets.url") + config.getString("assets.version")
	lazy val ersUrl: String = config.baseUrl("ers-returns")
	lazy val validatorUrl: String = config.baseUrl("ers-file-validator")

	lazy val upscanProtocol: String = config.getConfString("upscan.protocol","http").toLowerCase()
	lazy val upscanInitiateHost: String = config.baseUrl("upscan")
	lazy val upscanRedirectBase: String = config.getString("microservice.services.upscan.redirect-base")

	lazy val urBannerToggle: Boolean = config.getBoolean("urBanner.toggle")
	lazy val urBannerLink: String = config.getString("urBanner.link")
	lazy val ggSignInUrl: String = config.getString("government-gateway-sign-in.host")

	lazy val enableRetrieveSubmissionData: Boolean = config.getBoolean("settings.enable-retrieve-submission-data")
	lazy val languageTranslationEnabled: Boolean = config.getConfBool("features.welsh-translation", defBool = true)

	lazy val odsSuccessRetryAmount: Int = config.getInt("retry.ods-success-cache.complete-upload.amount")
	lazy val odsValidationRetryAmount: Int = config.getInt("retry.ods-success-cache.validation.amount")
	lazy val allCsvFilesCacheRetryAmount: Int = config.getInt("retry.csv-success-cache.all-files-complete.amount")
	lazy val retryDelay: FiniteDuration = FiniteDuration(config.getString("retry.delay").toInt, "ms")
	lazy val accessThreshold: Int = config.getInt("accessThreshold")
	lazy val timeOutSeconds : Int = config.getInt("sessionTimeout.timeoutSeconds")
	lazy val timeOutCountDownSeconds: Int = config.getInt("sessionTimeout.time-out-countdown-seconds")
	lazy val timeOut = s"$loginCallback/signed-out"

	lazy val sentViaSchedulerNoOfRowsLimit: Int = 10000

	//Previous ExternalUrls Object
	lazy val companyAuthHost: String = config.getString(s"microservice.services.auth.company-auth.host")
	lazy val signOutCallback: String = config.getString(s"microservice.services.feedback-survey-frontend.url")
	lazy val signOut = s"$companyAuthHost/gg/sign-out?continue=$signOutCallback"
	lazy val loginCallback: String = config.getString(s"microservice.services.auth.login-callback.url")
	lazy val portalDomain: String = config.getString("portal.domain")
	lazy val hmacToken: String = config.getString("hmac.hmac_token")
	lazy val hmacOnSwitch: Boolean = config.getBoolean("hmac.hmac_switch")

	//SessionCacheWiring
	lazy val shortLivedCacheBaseUri: String = config.baseUrl("cachable.short-lived-cache")
	lazy val shortLivedCacheDomain: String = config.getString(s"microservice.services.cachable.short-lived-cache.domain")
	lazy val sessionCacheBaseUri: String = config.baseUrl("cachable.session-cache")
	lazy val sessionCacheDomain: String = config.getString(s"microservice.services.cachable.session-cache.domain")

	lazy val useNewValidator: Boolean = config.getBoolean("feature-flag.new-validator")

}
