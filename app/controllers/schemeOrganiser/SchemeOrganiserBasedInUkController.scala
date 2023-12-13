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
import controllers.subsidiaries.SubsidiaryBaseController
import models.{CompanyBasedInUk, CompanyDetails, RequestObject, RsFormMappings}
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{CountryCodes, ERSUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeOrganiserBasedInUkController @Inject()(val mcc: MessagesControllerComponents,
                                                   val authConnector: DefaultAuthConnector,
                                                   val ersConnector: ErsConnector,
                                                   val globalErrorView: views.html.global_error,
                                                   val authAction: AuthAction,
                                                   implicit val countryCodes: CountryCodes,
                                                   implicit val ersUtil: ERSUtil,
                                                   implicit val appConfig: ApplicationConfig,
                                                   pageView: views.html.manual_is_the_company_in_uk
                                             )
  extends FrontendController(mcc) with WithUnsafeDefaultFormBinding with SchemeOrganiserBaseController[CompanyBasedInUk] {

  implicit val ec: ExecutionContext = mcc.executionContext

  val cacheKey: String = ersUtil.SCHEME_ORGANISER_BASED
  implicit val format: Format[CompanyBasedInUk] = CompanyBasedInUk.format

  def nextPageRedirect(index: Int, edit: Boolean = false)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      subsidiaryBasedInUk <-  if (edit) {
        ersUtil.fetch[CompanyDetails](ersUtil.SCHEME_ORGANISER_CACHE, requestObject.getSchemeReference).map {
          companyDetails => CompanyBasedInUk(companyDetails.basedInUk)
        }
      } else {
        ersUtil.fetch[CompanyBasedInUk](cacheKey, requestObject.getSchemeReference)
      }
    } yield {
      (subsidiaryBasedInUk.basedInUk, edit) match {
        case (true, false) => Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserDetailsUkController.questionPage())
        case (false, false) => Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserDetailsOverseasController.questionPage())

        case (true, true) => Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserDetailsUkController.editCompany(index))
        case (false, true) => Redirect(controllers.schemeOrganiser.routes.SchemeOrganiserDetailsOverseasController.editCompany(index))
      }
    }
  }

  def form(implicit request: Request[AnyContent]): Form[CompanyBasedInUk] = RsFormMappings.companyBasedInUkForm()

  def view(requestObject: RequestObject, index: Int, companyBasedInUkForm: Form[CompanyBasedInUk], edit: Boolean = false)
          (implicit request: Request[AnyContent], hc: HeaderCarrier): Html = {
    pageView(requestObject, index, companyBasedInUkForm, edit, schemeOrganiser = true)
  }

}
