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

import javax.inject.Inject
import play.api.Play
import uk.gov.hmrc.crypto.{ApplicationCrypto, CryptoWithKeysFromConfig}
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

class ERSFileValidatorSessionCache @Inject()(val http: DefaultHttpClient,
																						 appConfig: ApplicationConfig
																						) extends SessionCache {
	lazy val defaultSource: String = appConfig.appName
	lazy val baseUri: String = appConfig.sessionCacheBaseUri
	lazy val domain: String = appConfig.sessionCacheDomain
}

class ERSShortLivedHttpCache @Inject()(val http: DefaultHttpClient,
																			 appConfig: ApplicationConfig
																			) extends ShortLivedHttpCaching {
	override lazy val defaultSource: String = appConfig.appName
	lazy val baseUri: String = appConfig.shortLivedCacheBaseUri
	lazy val domain: String = appConfig.shortLivedCacheDomain
}

class ERSShortLivedCache @Inject()(val http: DefaultHttpClient,
																	appConfig: ApplicationConfig
																 ) extends ShortLivedCache {
	def shortLiveCache: ShortLivedHttpCaching = new ERSShortLivedHttpCache(http, appConfig)
	override implicit lazy val crypto: CryptoWithKeysFromConfig = new ApplicationCrypto(Play.current.configuration.underlying).JsonCrypto
}
