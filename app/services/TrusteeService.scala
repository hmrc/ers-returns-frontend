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
import play.api.mvc.Request
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrusteeService @Inject()(
                              ersUtil: ERSUtil,
                              sessionService: FrontendSessionService
                              )(implicit ec: ExecutionContext) {

  def updateTrusteeCache(index: Int)(implicit request: Request[_]): Future[Unit] = {
    for {
      requestObject      <- sessionService.fetch[RequestObject](ersUtil.ERS_REQUEST_OBJECT)
      schemeRef          =  requestObject.getSchemeReference
      name               <- sessionService.fetch[TrusteeName](ersUtil.TRUSTEE_NAME_CACHE)
      cachedTrustees     <- sessionService.fetchTrusteesOptionally()
      trusteeDetailsList <- {
        sessionService.fetch[TrusteeAddress](ersUtil.TRUSTEE_ADDRESS_CACHE).map( address =>
        TrusteeDetailsList(replaceTrustee(cachedTrustees.trustees, index, TrusteeDetails(name, address))))
      }
      _ <- sessionService.cache[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, trusteeDetailsList)
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

}
