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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.upscan.{PreparedUpload, Reference, UploadForm, UpscanInitiateRequest}
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.WireMockHelper

import scala.concurrent.duration.SECONDS

class UpscanConnectorSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with GuiceOneAppPerSuite
    with MockitoSugar
    with WireMockHelper {

  lazy val connector: UpscanConnector         = app.injector.instanceOf[UpscanConnector]
  implicit val hc: HeaderCarrier              = HeaderCarrier()
  val request: UpscanInitiateRequest          =
    UpscanInitiateRequest("callbackUrl", "successRedirectUrl", "errorRedirectUrl", 1, 104857600)
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.upscan.port" -> server.port()
    )
    .build()

  "getUpscanFormData" should {
    "return a UpscanInitiateResponse" when {
      "upscan returns valid successful response" in {
        val body = PreparedUpload(Reference("Reference"), UploadForm("downloadUrl", Map("formKey" -> "formValue")))
        server.stubFor(
          post(urlEqualTo(connector.upscanInitiatePath))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(body).toString())
            )
        )

        val result = await(connector.getUpscanFormData(request), 1, SECONDS)
        result shouldBe body.toUpscanInitiateResponse
      }
    }

    "throw an exception" when {
      "upscan returns a 4xx response" in {
        server.stubFor(
          post(urlEqualTo(connector.upscanInitiatePath))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            )
        )
        val exception = intercept[UpstreamErrorResponse] {
          await(connector.getUpscanFormData(request), 1, SECONDS)
        }

        exception.statusCode shouldBe 400
      }

      "upscan returns 5xx response" in {
        server.stubFor(
          post(urlEqualTo(connector.upscanInitiatePath))
            .willReturn(
              aResponse()
                .withStatus(SERVICE_UNAVAILABLE)
            )
        )

        val exception = intercept[UpstreamErrorResponse] {
          await(connector.getUpscanFormData(request), 1, SECONDS)
        }

        exception.statusCode shouldBe 503
      }
    }
  }
}
