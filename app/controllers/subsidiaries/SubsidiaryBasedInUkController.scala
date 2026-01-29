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
import models.{CompanyBasedInUk, RequestObject, RsFormMappings}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc._
import play.twirl.api.Html
import services.FrontendSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubsidiaryBasedInUkController @Inject() (
  val mcc: MessagesControllerComponents,
  val authConnector: DefaultAuthConnector,
  val ersConnector: ErsConnector,
  val globalErrorView: views.html.global_error,
  val authAction: AuthAction,
  implicit val countryCodes: CountryCodes,
  implicit val ersUtil: ERSUtil,
  implicit val sessionService: FrontendSessionService,
  implicit val appConfig: ApplicationConfig,
  pageView: views.html.manual_is_the_company_in_uk
) extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with SubsidiaryBaseController[CompanyBasedInUk] {

  implicit val ec: ExecutionContext = mcc.executionContext

  val cacheKey: String                          = ersUtil.SUBSIDIARY_COMPANY_BASED
  implicit val format: Format[CompanyBasedInUk] = CompanyBasedInUk.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit
    hc: HeaderCarrier,
    request: RequestHeader
  ): Future[Result] =
    for {
      subsidiaryBasedInUk <- if (edit) {
                               sessionService.fetchCompaniesOptionally().map { companyDetailsList =>
                                 CompanyBasedInUk(companyDetailsList.companies(index).basedInUk)
                               }
                             } else { sessionService.fetch[CompanyBasedInUk](cacheKey) }
    } yield (subsidiaryBasedInUk.basedInUk, edit) match {
      case (true, false)  => Redirect(controllers.subsidiaries.routes.SubsidiaryDetailsUkController.questionPage())
      case (false, false) =>
        Redirect(controllers.subsidiaries.routes.SubsidiaryDetailsOverseasController.questionPage())

      case (true, true)  => Redirect(controllers.subsidiaries.routes.SubsidiaryDetailsUkController.editCompany(index))
      case (false, true) =>
        Redirect(controllers.subsidiaries.routes.SubsidiaryDetailsOverseasController.editCompany(index))
    }

  def form(implicit request: Request[AnyContent]): Form[CompanyBasedInUk] = RsFormMappings.companyBasedInUkForm()

  def view(
    requestObject: RequestObject,
    index: Int,
    companyBasedInUkForm: Form[CompanyBasedInUk],
    edit: Boolean = false
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Html =
    pageView(requestObject, index, companyBasedInUkForm, edit, schemeOrganiser = false)

}
