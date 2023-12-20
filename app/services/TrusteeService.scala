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

package services

import models._
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils.ERSUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrusteeService @Inject()(ersUtil: ERSUtil)(implicit ec: ExecutionContext) extends Logging {

  def updateTrusteeCache(index: Int)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      requestObject      <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      schemeRef          =  requestObject.getSchemeReference
      name               <- ersUtil.fetch[TrusteeName](ersUtil.TRUSTEE_NAME_CACHE, schemeRef)
      cachedTrustees     <- ersUtil.fetchTrusteesOptionally(schemeRef)
      trusteeDetailsList <- {
        ersUtil.fetch[TrusteeAddress](ersUtil.TRUSTEE_ADDRESS_CACHE, schemeRef).map{ address =>
        TrusteeDetailsList(replaceTrustee(cachedTrustees.trustees, index, TrusteeDetails(name, address)))}
      }
      _ <- ersUtil.cache[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, trusteeDetailsList, schemeRef)
    } yield {
      ()
    }
  }

  def replaceTrustee(trustees: List[TrusteeDetails], index: Int, formData: TrusteeDetails): List[TrusteeDetails] =

    (if (index == 10000) {
      trustees :+ formData
    } else {
      trustees.zipWithIndex.map{
        case (a, b) => if (b == index) formData else a
      }
    }).distinct

  def deleteTrustee(index: Int)(implicit hc: HeaderCarrier): Future[Boolean] = {
    (for {
      requestObject <- ersUtil.fetch[RequestObject](ersUtil.ersRequestObject)
      cachedTrusteeList <- ersUtil.fetch[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, requestObject.getSchemeReference)
      trusteeDetailsList = TrusteeDetailsList(filterDeletedTrustee(cachedTrusteeList, index))
      _ <- ersUtil.cache(ersUtil.TRUSTEES_CACHE, trusteeDetailsList, requestObject.getSchemeReference)
    } yield true).recover {
      case e: Throwable =>
        logger.warn(s"[TrusteeService][deleteTrustee] Deleting trustee failed: ${e.getMessage}")
        false
    }
  }

  private def filterDeletedTrustee(trusteeDetailsList: TrusteeDetailsList, id: Int): List[TrusteeDetails] =
    trusteeDetailsList.trustees.zipWithIndex.filterNot(_._2 == id).map(_._1)
}
