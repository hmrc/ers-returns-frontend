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

package services

import models._
import play.api.Logging
import play.api.mvc.RequestHeader
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrusteeService @Inject() (ersUtil: ERSUtil, sessionService: FrontendSessionService)(implicit ec: ExecutionContext)
    extends Logging {

  def updateTrusteeCache(index: Int)(implicit request: RequestHeader): Future[Unit] =
    (for {
        name               <- sessionService.fetch[TrusteeName](ersUtil.TRUSTEE_NAME_CACHE)
        cachedTrustees     <- sessionService.fetchTrusteesOptionally()
        trusteeDetailsList <-
          sessionService
            .fetch[TrusteeAddress](ersUtil.TRUSTEE_ADDRESS_CACHE)
            .map(address =>
              TrusteeDetailsList(replaceTrustee(cachedTrustees.trustees, index, TrusteeDetails(name, address)))
            )
        _                  <- sessionService.cache[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, trusteeDetailsList)
      } yield () // to be picked up in tech debt review
    ).recover { case ex: Throwable =>
      logger.error("[TrusteeService][updateTrusteeCache] Error updating trustee cache", ex)
      ()
    }

  def replaceTrustee(trustees: List[TrusteeDetails], index: Int, formData: TrusteeDetails): List[TrusteeDetails] =
    (if (index == 10000) {
       trustees :+ formData
     } else {
       trustees.zipWithIndex.map { case (a, b) =>
         if (b == index) formData else a
       }
     }).distinct

  def deleteTrustee(index: Int)(implicit request: RequestHeader): Future[Boolean] =
    (for {
      cachedTrusteeList <- sessionService.fetch[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE)
      trusteeDetailsList = TrusteeDetailsList(filterDeletedTrustee(cachedTrusteeList, index))
      _                 <- sessionService.cache(ersUtil.TRUSTEES_CACHE, trusteeDetailsList)
    } yield true).recover { case e: Throwable =>
      logger.warn(s"[TrusteeService][deleteTrustee] Deleting trustee failed: ${e.getMessage}")
      false
    }

  private def filterDeletedTrustee(trusteeDetailsList: TrusteeDetailsList, id: Int): List[TrusteeDetails] =
    trusteeDetailsList.trustees.zipWithIndex.filterNot(_._2 == id).map(_._1)

}
