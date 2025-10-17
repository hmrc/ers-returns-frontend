/*
 * Copyright 2025 HM Revenue & Customs
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

trait Constants {
  val LARGE_FILE_STATUS = "largefiles"
  val SAVED_STATUS = "saved"
  val ERS_METADATA = "ers-meta-data"
  val ERS_REQUEST_OBJECT = "ers-request-object"
  val REPORTABLE_EVENTS = "reportable-events"
  val GROUP_SCHEME_CACHE_CONTROLLER = "group-scheme-controller"
  val ALT_AMENDS_CACHE_CONTROLLER = "alt-amends-cache-controller"
  val CSV_FILES_CALLBACK_LIST = "csv-file-callback-List"
  val FILE_TYPE_CACHE = "check-file-type"
  val ALT_AMENDS_ACTIVITY = "alt-activity"
  val CHECK_CSV_FILES = "check-csv-files"
  val CSV_FILES_UPLOAD = "csv-files-upload"
  val FILE_NAME_CACHE = "file-name"
  val SCHEME_ORGANISER_CACHE = "scheme-organiser"
  val TRUSTEES_CACHE = "trustees"
  val TRUSTEE_NAME_CACHE = "trustee-name"
  val TRUSTEE_BASED_CACHE = "trustee-based"
  val TRUSTEE_ADDRESS_CACHE = "trustee-address"
  val BUNDLE_REF = "sap-bundle-ref"
  val VALIDATED_SHEETS = "validated-sheets"
  val SUBSIDIARY_COMPANY_NAME_CACHE: String = "subsidiary-company-name"
  val SUBSIDIARY_COMPANY_ADDRESS_CACHE: String = "subsidiary-company-address"
  val SUBSIDIARY_COMPANY_BASED: String = "subsidiary-company-based"
  val SUBSIDIARY_COMPANIES_CACHE: String = "subsidiary-companies"
  val COMPANIES: String = "companies"
  val SCHEME_ORGANISER_NAME_CACHE: String = "scheme-organiser-name"
  val SCHEME_ORGANISER_ADDRESS_CACHE: String = "scheme-organiser-address"
  val SCHEME_ORGANISER_BASED: String = "scheme-organiser-based"
}
