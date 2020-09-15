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

package utils

import models.{ErsMetaData, SchemeInfo}
import org.joda.time.DateTime
import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

class ErsMetaDataHelperSpec extends UnitSpec with MockitoSugar with Matchers with GuiceOneAppPerSuite {
  def injector: Injector = app.injector
  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(Map("play.i18n.langs"->List("en-GB","en","cy-GB", "cy"))).build()

  "ErsMetaDataHelper" should {

    implicit val messages: Messages = messagesApi.preferred(Seq(Lang.get("en").get))

    def metaData(schemeName: String, schemeType: String) = ErsMetaData(
      SchemeInfo("XA1100000000000", DateTime.now, "001", "2015/16", schemeName, schemeType),
      "ipRef",
      Some("aoRef"),
      "empRef",
      Some("agentRef"),
      Some("sapNumber")
    )
  }
}
