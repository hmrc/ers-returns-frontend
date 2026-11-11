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
import models.{Company, RequestObject, RsFormMappings}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc.*
import play.twirl.api.Html
import services.FrontendSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubsidiaryDetailsUkController @Inject() (
  val mcc: MessagesControllerComponents,
  val authConnector: DefaultAuthConnector,
  val ersConnector: ErsConnector,
  val globalErrorView: views.html.global_error,
  val authAction: AuthAction,
  companyUKNameView: views.html.manual_company_details_uk
)(using
  val countryCodes: CountryCodes,
  val ersUtil: ERSUtil,
  val sessionService: FrontendSessionService,
  val appConfig: ApplicationConfig
) extends FrontendController(mcc) with WithUrlEncodedOnlyFormBinding with SubsidiaryBaseController[Company] {

  given ec: ExecutionContext = mcc.executionContext

  val cacheKey: String = ersUtil.SUBSIDIARY_COMPANY_NAME_CACHE

  given format: Format[Company] = Company.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(using
    hc: HeaderCarrier,
    request: RequestHeader
  ): Future[Result] =
    if (edit) {
      Future.successful(Redirect(controllers.subsidiaries.routes.SubsidiaryAddressUkController.editCompany(index)))
    } else {
      Future.successful(Redirect(controllers.subsidiaries.routes.SubsidiaryAddressUkController.questionPage()))
    }

  def form(using request: Request[AnyContent]): Form[Company] = RsFormMappings.companyNameForm()

  def view(requestObject: RequestObject, index: Int, companyNameUKForm: Form[Company], edit: Boolean = false)(using
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Html =
    companyUKNameView(requestObject, index, companyNameUKForm, edit, schemeOrganiser = false)

}
