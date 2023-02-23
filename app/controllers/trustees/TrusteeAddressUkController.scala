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
import controllers.auth.AuthAction
import javax.inject.Inject
import models.{RequestObject, RsFormMappings, TrusteeAddressUk}
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

class TrusteeAddressUkController @Inject()(val mcc: MessagesControllerComponents,
                                           val authConnector: DefaultAuthConnector,
                                           val ersConnector: ErsConnector,
                                           val globalErrorView: views.html.global_error,
                                           val authAction: AuthAction,
                                           val trusteeService: TrusteeService,
                                           implicit val countryCodes: CountryCodes,
                                           implicit val ersUtil: ERSUtil,
                                           implicit val appConfig: ApplicationConfig,
                                           trusteeAddressUkView: views.html.trustee_address_uk
                                          )
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with TrusteeBaseController[TrusteeAddressUk] {

  implicit val ec: ExecutionContext = mcc.executionContext

  val cacheKey: String = ersUtil.TRUSTEE_ADDRESS_CACHE
  implicit val format: Format[TrusteeAddressUk] = TrusteeAddressUk.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier): Future[Result] = {
    trusteeService.updateTrusteeCache(index).map { _ =>
      Redirect(controllers.routes.TrusteeController.trusteeSummaryPage())
      //TODO Update this to the next page in the journey innit (might be right now, until I move stuff to new summary controller or whatever)
    }
  }

  def form(implicit request: Request[AnyContent]): Form[TrusteeAddressUk] = RsFormMappings.trusteeAddressUkForm()

  def view(requestObject: RequestObject, groupSchemeActivity: String, index: Int, trusteeAddressUkForm: Form[TrusteeAddressUk], edit: Boolean = false)
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html = {
    trusteeAddressUkView(requestObject, groupSchemeActivity, index, trusteeAddressUkForm, edit)
  }

}
