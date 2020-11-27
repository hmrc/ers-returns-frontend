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

package controllers

import java.net.URI

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe.Try

class LanguageSwitchController @Inject() (appConfig: ApplicationConfig,
                                          val mcc: MessagesControllerComponents
                                         ) extends FrontendController(mcc) with I18nSupport {

  implicit val ec: ExecutionContext = mcc.executionContext

  private def fallbackURL: String = routes.ReturnServiceController.hmacCheck().url

  private def languageMap: Map[String, Lang] = appConfig.languageMap

  def switchToLanguage(language: String): Action[AnyContent] = Action { implicit request =>

  val enabled = appConfig.languageTranslationEnabled
  val lang = if (enabled) {
    languageMap.getOrElse(language, Lang.defaultLang)
  } else {
    Lang("en")
  }
    val redirectURL = request.headers.get(REFERER)
      .flatMap(asRelativeUrl)
      .getOrElse(fallbackURL)

    Redirect(redirectURL).withLang(Lang.apply(lang.code))
  }

  private def asRelativeUrl(url: String): Option[String] =

    for {
      uri      <- Option(new URI(url))
      path     <- Option(uri.getPath).filterNot(_.isEmpty)
      query    <- Option(uri.getQuery).map("?" + _).orElse(Some(""))
      fragment <- Option(uri.getRawFragment).map("#" + _).orElse(Some(""))
    } yield s"$path$query$fragment"

}


