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

import javax.inject.Inject
import models.{RequestObject, TrusteeAddress, TrusteeDetails, TrusteeDetailsList, TrusteeName}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import utils.ERSUtil
import services.ERSSessionCacheService

import scala.concurrent.{ExecutionContext, Future}

class TrusteeService @Inject()(
                              ersUtil: ERSUtil,
                              ersSessionCacheService: ERSSessionCacheService
                              )(implicit ec: ExecutionContext) {

  def updateTrusteeCache(index: Int)(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] = {
    for {
      requestObject      <- ersSessionCacheService.fetch[RequestObject](ersUtil.ersRequestObject)
      schemeRef          =  requestObject.getSchemeReference
      name               <- ersSessionCacheService.fetch[TrusteeName](ersUtil.TRUSTEE_NAME_CACHE, schemeRef)
      cachedTrustees     <- ersSessionCacheService.fetchTrusteesOptionally(schemeRef)
      trusteeDetailsList <- {
        ersSessionCacheService.fetch[TrusteeAddress](ersUtil.TRUSTEE_ADDRESS_CACHE, schemeRef).map( address =>
        TrusteeDetailsList(replaceTrustee(cachedTrustees.trustees, index, TrusteeDetails(name, address))))
      }
      _ <- ersSessionCacheService.cache[TrusteeDetailsList](ersUtil.TRUSTEES_CACHE, trusteeDetailsList, schemeRef)
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
