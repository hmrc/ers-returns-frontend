/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import models.{RsFormMappings, _}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckFileTypeController @Inject()(val mcc: MessagesControllerComponents,
																				val authConnector: DefaultAuthConnector,
																				implicit val ersUtil: ERSUtil,
																				implicit val appConfig: ApplicationConfig,
                                        globalErrorView: views.html.global_error,
                                        checkFileTypeView: views.html.check_file_type
                                       ) extends FrontendController(mcc) with Authenticator with I18nSupport with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def checkFileTypePage(): Action[AnyContent] = authorisedByGG {
    implicit authContext =>
      implicit request =>
          showCheckFileTypePage()(authContext, request, hc)
  }

  def showCheckFileTypePage()(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    (for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      fileType      <- ersUtil.fetch[CheckFileType](ersUtil.FILE_TYPE_CACHE, requestObject.getSchemeReference).recover{
        case _: NoSuchElementException => CheckFileType(Some(""))
      }
    } yield {
      Ok(checkFileTypeView(requestObject, fileType.checkFileType, RsFormMappings.checkFileTypeForm.fill(fileType)))
    }).recover{
      case e: Throwable =>
        logger.error(s"[CheckFileTypeController][showCheckFileTypePage] Rendering CheckFileType view failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        getGlobalErrorPage
    }
  }

  def checkFileTypeSelected(): Action[AnyContent] = authorisedByGG {
    implicit authContext: ERSAuthData =>
      implicit request =>
          showCheckFileTypeSelected()(request, hc)
  }

  def showCheckFileTypeSelected()(implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = {
    ersUtil.fetch[RequestObject](ersUtil.ersRequestObject).flatMap { requestObject =>
      RsFormMappings.checkFileTypeForm.bindFromRequest.fold(
        errors => {
          Future.successful(Ok(checkFileTypeView(requestObject, Some(""), errors)))
        },
        formData => {
          ersUtil.cache(ersUtil.FILE_TYPE_CACHE, formData, requestObject.getSchemeReference).map { _ =>
            if (formData.checkFileType.contains(ersUtil.OPTION_ODS)) {
              Redirect(routes.FileUploadController.uploadFilePage())
            } else {
              Redirect(routes.CheckCsvFilesController.checkCsvFilesPage())
            }
          }.recover {
            case e: Exception =>
              logger.error("[CheckFileTypeController][showCheckFileTypeSelected] Unable to save file type. Error: " + e.getMessage)
              getGlobalErrorPage
          }
        }
      )
    }
  }

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(globalErrorView(
			"ers.global_errors.title",
			"ers.global_errors.heading",
			"ers.global_errors.message"
		)(request, messages, appConfig))
	}
}
