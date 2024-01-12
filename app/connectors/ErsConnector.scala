/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import config.ApplicationConfig
import controllers.auth.RequestWithOptionalAuthContext
import metrics.Metrics
import models._
import models.upscan.{UploadStatus, UploadedSuccessfully}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue}
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HttpReads.Implicits
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErsConnector @Inject() (val http: DefaultHttpClient, appConfig: ApplicationConfig)(implicit
  ec: ExecutionContext
) extends Logging with Metrics {

  lazy val ersUrl: String = appConfig.ersUrl
  lazy val validatorUrl: String = appConfig.validatorUrl
  implicit val rds: HttpReads[HttpResponse] = Implicits.readRaw

  def connectToEtmpSapRequest(
    schemeRef: String
  )(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[String] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/sapRequest/" + schemeRef
    val startTime = System.currentTimeMillis()
    http
      .GET[HttpResponse](url)
      .map { response =>
        response.status match {
          case OK =>
            val sapNumber: String = (response.json \ "SAP Number").as[String]
            sapNumber
          case _  =>
            logger.error(
              s"[ErsConnector][connectToEtmpSapRequest] SAP request failed with status ${response.status}, timestamp: ${System.currentTimeMillis()}."
            )
            throw new Exception
        }
      }
      .recover { case e: Exception =>
        logger.error(
          s"[ErsConnector][connectToEtmpSapRequest] connectToEtmpSapRequest failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
        )
        ersConnector(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        throw new Exception
      }
  }

  def connectToEtmpSummarySubmit(sap: String, payload: JsValue)(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[String] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/summarySubmit/" + sap
    http.POST(url, payload).map { res =>
      res.status match {
        case OK =>
          val bundleRef: String = (res.json \ "Form Bundle Number").as[String]
          bundleRef
        case _  =>
          logger.error(
            s"[ErsConnector][connectToEtmpSummarySubmit] Summary submit request failed with status ${res.status}, timestamp: ${System.currentTimeMillis()}."
          )
          throw new Exception
      }
    }
  }

  def submitReturnToBackend(
    allData: ErsSummary
  )(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/saveReturnData"
    http.POST(url, allData)
  }

  def validateFileData(callbackData: UploadedSuccessfully, schemeInfo: SchemeInfo)(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$validatorUrl/ers/$empRef/process-file"
    val startTime = System.currentTimeMillis()
    logger.debug("[ErsConnector][connectToEtmpSapRequest] validateFileData: Call to Validator: " + (System.currentTimeMillis() / 1000))
    http
      .POST(url, ValidatorData(callbackData, schemeInfo))
      .map { res =>
        ersConnector(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        res.status match {
          case OK         => res
          case ACCEPTED   => res
          case NO_CONTENT => res
          case _          => throw new Exception(s"[ErsConnector][validateFileData] Received status code ${res.status} from file validator")
        }
      }
      .recover { case e: Exception =>
        logger.error(
          s"[ErsConnector][validateFileData] Validate file data failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
        )
        ersConnector(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        HttpResponse(BAD_REQUEST, "")
      }
  }

  def validateCsvFileData(callbackData: List[UploadedSuccessfully], schemeInfo: SchemeInfo)(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$validatorUrl/ers/v2/$empRef/process-csv-file"

    http.POST(url, CsvValidatorData(callbackData, schemeInfo)).map { res =>
      res.status match {
        case OK => res
        case ACCEPTED => res
        case NO_CONTENT => res
        case _ => throw new Exception(s"[ErsConnector][validateCsvFileData] Received status code ${res.status} from file validator")
      }
    } recover { case e: Exception =>
      logger.error(
        s"[ErsConnector][validateCsvFileData] Validate file data failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}."
      )
      HttpResponse(BAD_REQUEST, "")
    }
  }

  def saveMetadata(
    allData: ErsSummary
  )(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/saveMetadata"
    http.POST(url, allData)
  }

  def checkForPresubmission(schemeInfo: SchemeInfo, validatedSheets: String)(implicit
    request: RequestWithOptionalAuthContext[AnyContent],
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/check-for-presubmission/$validatedSheets"
    http.POST(url, schemeInfo)
  }

  def removePresubmissionData(
    schemeInfo: SchemeInfo
  )(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/removePresubmissionData"
    http.POST(url, schemeInfo)
  }

  def retrieveSubmissionData(
    data: JsObject
  )(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = request.authData.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/retrieve-submission-data"
    http.POST(url, data)
  }

  def createCallbackRecord(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Int] = {
    withOptionalSession(
      ifSome = { sessionId =>
        val url: String = s"$validatorUrl/ers/$sessionId/create-callback"
        http.POSTEmpty(url).map {
          case response if response.status == CREATED => CREATED
          case response =>
            logger.error(s"[ErsConnector][createCallbackRecord] Received unexpected status code ${response.status} from file validator")
            throw new Exception(s"[ErsConnector][createCallbackRecord] Unexpected response status code ${response.status}")
        }.recover {
          case ex: Exception =>
            logger.error("[ErsConnector][createCallbackRecord] Error in POST request: " + ex.getMessage, ex)
            throw ex
        }
      },
      ifNone = Future.failed(new NoSuchElementException("[ErsConnector][createCallbackRecord] Session ID not found in the request session"))
    )
  }

  def updateCallbackRecord(uploadStatus: UploadStatus, sessionId: String)(implicit hc: HeaderCarrier): Future[Int] = {
    val url: String = s"$validatorUrl/ers/$sessionId/update-callback"

    http.PUT(url, uploadStatus).map {
      case response if response.status == 204 => NO_CONTENT
      case response =>
        logger.error(s"[ErsConnector][updateCallbackRecord] Received unexpected status code ${response.status} from file validator")
        throw new Exception(s"[ErsConnector][updateCallbackRecord] Unexpected response status code ${response.status}")
    }
  }

  def getCallbackRecord(implicit request: RequestWithOptionalAuthContext[AnyContent], hc: HeaderCarrier): Future[Option[UploadStatus]] = {
    withOptionalSession(
      ifSome = { sessionId =>
        val url: String = s"$validatorUrl/ers/$sessionId/get-callback"
        http.GET[HttpResponse](url).map {
          case response if response.status == OK =>
            response.json.validate[UploadStatus] match {
              case JsSuccess(uploadStatus, _) => Some(uploadStatus)
              case JsError(errors) =>
                logger.error("[ErsConnector][getCallbackRecord] Error parsing UploadStatus: " + errors.toString)
                None
            }
          case response =>
            logger.error(s"[ErsConnector][getCallbackRecord] Unexpected response status code ${response.status} from file validator")
            None
        }.recover {
          case ex: Exception =>
            logger.error("[ErsConnector][getCallbackRecord] Error in GET request: " + ex.getMessage, ex)
            None
        }
      },
      ifNone = Future.successful(None)
    )
  }

  def withOptionalSession[A](ifSome: String => Future[A], ifNone: => Future[A])
                            (implicit request: RequestWithOptionalAuthContext[AnyContent]): Future[A] = {
    request.session.get("sessionId") match {
      case Some(sessionId) => ifSome(sessionId)
      case None => ifNone
    }
  }
}
