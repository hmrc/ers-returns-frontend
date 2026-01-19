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

import models.RequestObject
import models.upscan.{Reference, UpscanInitiateResponse}

trait FileUploadFixtures {

  val upscanInitiateResponse: UpscanInitiateResponse =
    UpscanInitiateResponse(
      fileReference = Reference("a1a47ace-cf71-4ad7-b2b1-2f22c1097aef"),
      postTarget = "http://localhost:9570/upscan/upload-proxy",
      formFields = Map("not" -> "used")
    )

  val testRequestObject: RequestObject =
    RequestObject(
      aoRef = Some("123PA12345678"),
      taxYear = Some("2014/15"),
      ersSchemeRef = Some("XA1100000000000"),
      schemeName = Some("MyScheme"),
      schemeType = None,
      agentRef = None,
      empRef = None,
      ts = None,
      hmac = Some("qlQmNGgreJRqJroWUUu0MxLq2oo=")
    )

}
