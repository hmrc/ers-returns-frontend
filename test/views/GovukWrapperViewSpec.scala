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

package views

import config.ApplicationConfig
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.govuk_wrapper

import scala.jdk.CollectionConverters._

class GovukWrapperViewSpec extends ViewSpecBase {

  private val view = app.injector.instanceOf[govuk_wrapper]

  implicit val appConfig: ApplicationConfig                 = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  implicit val messages: Messages                           = testMessages

  private val exitSurveyUrl =
    "http://localhost:9553/gg/sign-out?continue=http://localhost:9514/feedback/ERS?useServiceNavigation"

  private def linkHrefs(html: Html): Seq[String] =
    asDocument(html).select("a").eachAttr("href").asScala.toSeq

  "govuk_wrapper" should {
    "point the header sign out link at the feedback exit survey" in {
      linkHrefs(view("test page")(Html("<p>content</p>"))) must contain(exitSurveyUrl)
    }

    "omit the sign out link when sign out is disabled" in {
      val hrefs = linkHrefs(view("test page", disableSignOut = true)(Html("<p>content</p>")))
      hrefs must not be empty
      hrefs must not contain exitSurveyUrl
    }
  }

}
