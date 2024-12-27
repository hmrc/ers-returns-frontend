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

package controllers.subsidiaries

import controllers.auth.RequestWithOptionalAuthContext
import controllers.schemeOrganiser.SchemeOrganiserBaseController
import models.{CompanyDetailsList, RequestObject}
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait SubsidiaryBaseController[A] extends SchemeOrganiserBaseController[A] {

 override def submissionHandler(requestObject: RequestObject, index: Int, edit: Boolean = false)
                       (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      errors => {
        Future.successful(BadRequest(view(requestObject, index, errors, edit)))
      },
      result => {
        if (edit) {
          sessionService.fetchCompaniesOptionally().flatMap { companies =>
            val updatedCompany = companies.companies(index).updatePart(result)
            val updatedCompanies = CompanyDetailsList(companies.companies.updated(index, updatedCompany))
            sessionService.cache[CompanyDetailsList](sessionService.SUBSIDIARY_COMPANIES_CACHE, updatedCompanies).flatMap{ _ =>
              nextPageRedirect(index, edit)
            }
          }
        } else {
          sessionService.cache[A](cacheKey, result).flatMap { _ =>
            nextPageRedirect(index, edit)
          }
        }
      }
    ).recover {
      case e: Exception =>
        logger.error(s"[${this.getClass.getSimpleName}][submissionHandler] Error occurred while updating company cache: ${e.getMessage}")
        getGlobalErrorPage
    }
  }

  override def showQuestionPage(requestObject: RequestObject, index: Int, edit: Boolean = false)
                               (implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Result] = {
    sessionService.fetchPartFromCompanyDetailsList[A](index).map { previousAnswer: Option[A] =>
      val preparedForm = previousAnswer.fold(form)(form.fill(_))
      Ok(view(requestObject, index, preparedForm, edit))
    } recover {
      case e: Exception =>
        logger.error(s"[${this.getClass.getSimpleName}][showQuestionPage] Get data from cache failed with exception ${e.getMessage}")
        getGlobalErrorPage
    }
  }

}
