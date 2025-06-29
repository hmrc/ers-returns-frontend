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

package controllers.schemeOrganiser

import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.AuthAction
import models.{Company, RequestObject, RsFormMappings}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, RequestHeader}
import play.twirl.api.Html
import services.FrontendSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeOrganiserDetailsUkController @Inject()(val mcc: MessagesControllerComponents,
                                                   val ersConnector: ErsConnector,
                                                   val globalErrorView: views.html.global_error,
                                                   val authAction: AuthAction,
                                                   implicit val countryCodes: CountryCodes,
                                                   implicit val ersUtil: ERSUtil,
                                                   implicit val sessionService: FrontendSessionService,
                                                   implicit val appConfig: ApplicationConfig,
                                                   companyUKNameView: views.html.manual_company_details_uk
                                                  )
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with SchemeOrganiserBaseController[Company] {

  implicit val ec: ExecutionContext = mcc.executionContext

  val cacheKey: String = ersUtil.SCHEME_ORGANISER_NAME_CACHE

  implicit val format: Format[Company] = Company.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier, request: RequestHeader) = {
    if (edit) {
      Future.successful(Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserAddressUkController.editCompany(index)))
    } else {
      Future.successful(Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserAddressUkController.questionPage()))
    }
  }

  def form(implicit request: Request[AnyContent]): Form[Company] = RsFormMappings.companyNameForm(isSchemeOrganiser = true)

  def view(requestObject: RequestObject, index: Int, companyNameUKForm: Form[Company], edit: Boolean = false)
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html = {
    companyUKNameView(requestObject, index, companyNameUKForm, edit, schemeOrganiser = true)
  }

}
