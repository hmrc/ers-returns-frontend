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

package controllers.trustees

import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.AuthAction
import models.{RequestObject, RsFormMappings, TrusteeName}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc._
import play.twirl.api.Html
import services.{FrontendSessionService, TrusteeService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ERSUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrusteeNameController @Inject()(val mcc: MessagesControllerComponents,
                                      val ersConnector: ErsConnector,
                                      val globalErrorView: views.html.global_error,
                                      val authAction: AuthAction,
                                      val trusteeService: TrusteeService,
                                      val sessionService: FrontendSessionService,
                                      trusteeNameView: views.html.trustee_name)
                                     (implicit val ec: ExecutionContext,
                                      val ersUtil: ERSUtil,
                                      val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with TrusteeBaseController[TrusteeName] {

  implicit val format: Format[TrusteeName] = TrusteeName.format

  val cacheKey: String = ersUtil.TRUSTEE_NAME_CACHE

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier, request: RequestHeader): Future[Result] = {
    if (edit) {
      Future.successful(Redirect(controllers.trustees.routes.TrusteeBasedInUkController.editQuestion(index)))
    } else {
      Future.successful(Redirect(controllers.trustees.routes.TrusteeBasedInUkController.questionPage()))
    }
  }

  def form(implicit request: Request[AnyContent]): Form[TrusteeName] = RsFormMappings.trusteeNameForm()

  def view(requestObject: RequestObject, index: Int, trusteeNameForm: Form[TrusteeName], edit: Boolean = false)
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html = {
    trusteeNameView(requestObject, index, trusteeNameForm, edit)
  }

}
