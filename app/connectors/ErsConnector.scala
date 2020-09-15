/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import config.ApplicationConfig
import javax.inject.{Inject, Singleton}
import metrics.Metrics
import models._
import models.upscan.UploadedSuccessfully
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import play.api.http.Status._
import utils.ERSUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ErsConnector @Inject()(val http: DefaultHttpClient,
														 ersUtil: ERSUtil,
														 appConfig: ApplicationConfig
														) {

  lazy val metrics: Metrics = ersUtil
	lazy val ersUrl: String = appConfig.ersUrl
	lazy val validatorUrl: String = appConfig.validatorUrl

  def connectToEtmpSapRequest(schemeRef: String)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[String] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/sapRequest/" + schemeRef
		val startTime = System.currentTimeMillis()
    http.GET[HttpResponse](url).map { response =>
      response.status match {
        case OK =>
          val sapNumber: String = (response.json \ "SAP Number").as[String]
          sapNumber
        case _ =>
          Logger.error(s"SAP request failed with status ${response.status}, timestamp: ${System.currentTimeMillis()}.")
          throw new Exception
      }
    }.recover {
      case e: Exception =>
        Logger.error(s"connectToEtmpSapRequest failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        metrics.ersConnector(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        throw new Exception
    }
  }

  def connectToEtmpSummarySubmit(sap: String, payload: JsValue)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[String] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/summarySubmit/" + sap
    http.POST(url, payload).map { res =>
      res.status match {
        case OK =>
          val bundleRef: String = (res.json \ "Form Bundle Number").as[String]
          bundleRef
        case _ =>
          Logger.error(s"Summary submit request failed with status ${res.status}, timestamp: ${System.currentTimeMillis()}.")
          throw new Exception
      }
    }
  }

  def submitReturnToBackend(allData: ErsSummary)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/saveReturnData"
    http.POST(url, allData)
  }

  def validateFileData(callbackData: UploadedSuccessfully, schemeInfo: SchemeInfo)
											(implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$validatorUrl/ers/$empRef/process-file"
    val startTime = System.currentTimeMillis()
    Logger.debug("validateFileData: Call to Validator: " + (System.currentTimeMillis() / 1000))
    http.POST(url, ValidatorData(callbackData, schemeInfo)).map { res =>
      metrics.ersConnector(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
      res
    }.recover {
      case e: Exception =>
        Logger.error(s"validateFileData: Validate file data failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
        metrics.ersConnector(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        HttpResponse(BAD_REQUEST)
    }
  }

  def validateCsvFileData(callbackData: List[UploadedSuccessfully], schemeInfo: SchemeInfo)
                         (implicit authContext: ERSAuthData, request: Request[AnyRef], hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$validatorUrl/ers/$empRef/process-csv-file"
    http.POST(url, CsvValidatorData(callbackData, schemeInfo)) recover {
      case e: Exception =>
				Logger.error(s"validateCsvFileData: Validate file data failed with exception ${e.getMessage}, timestamp: ${System.currentTimeMillis()}.")
				HttpResponse(BAD_REQUEST)
		}
  }

  def saveMetadata(allData: ErsSummary)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/saveMetadata"
    http.POST(url, allData)
  }

  def checkForPresubmission(schemeInfo: SchemeInfo, validatedSheets: String)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/check-for-presubmission/$validatedSheets"
    http.POST(url, schemeInfo)
  }

  def removePresubmissionData(schemeInfo: SchemeInfo)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/removePresubmissionData"
    http.POST(url, schemeInfo)
  }

  def retrieveSubmissionData(data: JsObject)(implicit authContext: ERSAuthData, hc: HeaderCarrier): Future[HttpResponse] = {
    val empRef: String = authContext.empRef.encodedValue
    val url: String = s"$ersUrl/ers/$empRef/retrieve-submission-data"
    http.POST(url, data)
  }
}
