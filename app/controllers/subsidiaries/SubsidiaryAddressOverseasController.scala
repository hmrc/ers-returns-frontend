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

package controllers.subsidiaries

import config.ApplicationConfig
import connectors.ErsConnector
import controllers.auth.AuthAction
import models.{CompanyAddress, RequestObject, RsFormMappings}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc._
import play.twirl.api.Html
import services.{CompanyDetailsService, FrontendSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubsidiaryAddressOverseasController  @Inject()(val mcc: MessagesControllerComponents,
                                                     val authConnector: DefaultAuthConnector,
                                                     val ersConnector: ErsConnector,
                                                     val globalErrorView: views.html.global_error,
                                                     val authAction: AuthAction,
                                                     implicit val countryCodes: CountryCodes,
                                                     implicit val ersUtil: ERSUtil,
                                                     implicit val sessionService: FrontendSessionService,
                                                     implicit val appConfig: ApplicationConfig,
                                                     companyDetailsService: CompanyDetailsService,
                                                     companyAddressOverseasView: views.html.manual_address_overseas
                                                 )
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with SubsidiaryBaseController[CompanyAddress] {

  implicit val ec: ExecutionContext = mcc.executionContext

  val cacheKey: String = ersUtil.SUBSIDIARY_COMPANY_ADDRESS_CACHE
  implicit val format: Format[CompanyAddress] = CompanyAddress.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier, request: RequestHeader): Future[Result] = {
    if (edit) {
      Future.successful(Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage()))
    } else {
      companyDetailsService.updateSubsidiaryCompanyCache(index)
      Future.successful(Redirect(controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage()))
      }
    }

  def form(implicit request: Request[AnyContent]): Form[CompanyAddress] = RsFormMappings.companyAddressOverseasForm()

  def view(requestObject: RequestObject, index: Int, companyAddressOverseasForm: Form[CompanyAddress], edit: Boolean = false)
            (implicit request: Request[AnyContent], hc: HeaderCarrier): Html = {
      companyAddressOverseasView(requestObject, index, companyAddressOverseasForm, edit, schemeOrganiser = false)
  }

}
