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

package controllers.trustees

import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.{AuthAction, RequestWithOptionalAuthContext}
import javax.inject.Inject
import models.{GroupSchemeInfo, RequestObject, RsFormMappings, TrusteeBasedInUk}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import services.TrusteeService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import scala.concurrent.{ExecutionContext, Future}

class TrusteeBasedInUkController @Inject()(val mcc: MessagesControllerComponents,
                                           val authConnector: DefaultAuthConnector,
                                           val ersConnector: ErsConnector,
                                           val globalErrorView: views.html.global_error,
                                           val authAction: AuthAction,
                                           val trusteeService: TrusteeService,
                                           implicit val countryCodes: CountryCodes,
                                           implicit val ersUtil: ERSUtil,
                                           implicit val appConfig: ApplicationConfig,
                                           trusteeBasedInUkView: views.html.trustee_based_in_uk
                                      )
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with TrusteeBaseController[TrusteeBasedInUk] {

  implicit val ec: ExecutionContext = mcc.executionContext

  val cacheKey: String = ersUtil.TRUSTEE_BASED_CACHE
  implicit val format: Format[TrusteeBasedInUk] = TrusteeBasedInUk.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      trusteeBasedInUk <- ersUtil.fetch[TrusteeBasedInUk](cacheKey, requestObject.getSchemeReference)
    } yield {
      (trusteeBasedInUk.basedInUk, edit) match {
        case (true, true)    => Redirect(controllers.trustees.routes.TrusteeAddressUkController.editQuestion(index))
        case (true, false)   => Redirect(controllers.trustees.routes.TrusteeAddressUkController.questionPage())
        case (false, true)   => Redirect(controllers.trustees.routes.TrusteeAddressOverseasController.editQuestion(index))
        case (false, false)  => Redirect(controllers.trustees.routes.TrusteeAddressOverseasController.questionPage())
      }
    }
  }

  def form(implicit request: Request[AnyContent]): Form[TrusteeBasedInUk] = RsFormMappings.trusteeBasedInUkForm()

  def view(requestObject: RequestObject, groupSchemeActivity: String, index: Int, trusteeBasedInUkForm: Form[TrusteeBasedInUk], edit: Boolean = false)
                   (implicit request: Request[AnyContent], hc: HeaderCarrier): Html = {
    trusteeBasedInUkView(requestObject, groupSchemeActivity, index, trusteeBasedInUkForm, edit)
  }
}
