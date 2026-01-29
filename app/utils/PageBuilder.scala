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

package utils

import controllers.routes
import models.CsvFiles
import play.api.i18n.Messages

trait PageBuilder {

  val DEFAULT         = ""
  val DEFAULT_COUNTRY = "UK"
  val OVERSEAS        = "Overseas"

  // Schemes
  val CSOP: String  = "CSOP"
  val EMI: String   = "EMI"
  val SAYE: String  = "SAYE"
  val SIP: String   = "SIP"
  val OTHER: String = "OTHER"

  // Schemes
  val SCHEME_CSOP: String  = "1"
  val SCHEME_EMI: String   = "2"
  val SCHEME_SAYE: String  = "4"
  val SCHEME_SIP: String   = "5"
  val SCHEME_OTHER: String = "3"

  // Files
  val FILE_CSOP_GRANTED: String   = "file0"
  val FILE_CSOP_RCL: String       = "file1"
  val FILE_CSOP_Exercised: String = "file2"

  val FILE_EMI_ADJUSTMENTS: String = "file0"
  val FILE_EMI_REPLACED: String    = "file1"
  val FILE_EMI_RCL: String         = "file2"
  val FILE_EMI_NONTAXABLE: String  = "file3"
  val FILE_EMI_TAXABLE: String     = "file4"

  val FILE_SAYE_GRANTED: String   = "file0"
  val FILE_SAYE_RCL: String       = "file1"
  val FILE_SAYE_EXERCISED: String = "file2"

  val FILE_SIP_AWARDS: String = "file0"
  val FILE_SIP_OUT: String    = "file1"

  val FILE_OTHER_GRANTS: String       = "file0"
  val FILE_OTHER_OPTIONS: String      = "file1"
  val FILE_OTHER_ACQUISITION: String  = "file2"
  val FILE_OTHER_RESTRICTED: String   = "file3"
  val FILE_OTHER_BENEFITS: String     = "file4"
  val FILE_OTHER_CONVERTABLE: String  = "file5"
  val FILE_OTHER_NOTIONAL: String     = "file6"
  val FILE_OTHER_ENCHANCEMENT: String = "file7"
  val FILE_OTHER_SOLD: String         = "file8"

  // pageId's
  val PAGE_START                  = "ers_start"
  val PAGE_CHOOSE                 = "ers_choose"
  val PAGE_CHECK_CSV_FILE         = "ers_check_csv_file"
  val PAGE_CONFIRMATION           = "ers_confirmation"
  val PAGE_ALT_ACTIVITY           = "ers_alt_activity"
  val PAGE_ALT_AMENDS             = "ers_alt_amends"
  val PAGE_GROUP_ACTIVITY         = "ers_group_activity"
  val PAGE_SUMMARY_DECLARATION    = "ers_summary_declaration"
  val PAGE_GROUP_SUMMARY          = "ers_group_summary"
  val PAGE_MANUAL_COMPANY_DETAILS = "ers_manual_company_details"
  val PAGE_SCHEME_ORGANISER       = "ers_scheme_organiser"
  val PAGE_TRUSTEE_DETAILS        = "ers_trustee_details"
  val PAGE_TRUSTEE_BASED          = "ers_trustee_based"
  val PAGE_CONFIRM_DELETE_COMPANY = "ers.group_confirm_delete_company"

  // Options
  val OPTION_YES                = "1"
  val OPTION_NO                 = "2"
  val OPTION_UPLOAD_SPREEDSHEET = "1"
  val OPTION_NIL_RETURN         = "2"
  val OPTION_ODS                = "ods"
  val OPTION_CSV                = "csv"
  val OPTION_MANUAL             = "man"

  // message file entry prefix
  val MSG_CSOP: String  = ".csop."
  val MSG_EMI: String   = ".emi."
  val MSG_SAYE: String  = ".saye."
  val MSG_SIP: String   = ".sip."
  val MSG_OTHER: String = ".other."

  val CSVFilesList = Map(
    (
      EMI,
      List(
        CsvFiles(FILE_EMI_ADJUSTMENTS),
        CsvFiles(FILE_EMI_REPLACED),
        CsvFiles(FILE_EMI_RCL),
        CsvFiles(FILE_EMI_NONTAXABLE),
        CsvFiles(FILE_EMI_TAXABLE)
      )
    ),
    (
      CSOP,
      List(
        CsvFiles(FILE_CSOP_GRANTED),
        CsvFiles(FILE_CSOP_RCL),
        CsvFiles(FILE_CSOP_Exercised)
      )
    ),
    (
      OTHER,
      List(
        CsvFiles(FILE_OTHER_GRANTS),
        CsvFiles(FILE_OTHER_OPTIONS),
        CsvFiles(FILE_OTHER_ACQUISITION),
        CsvFiles(FILE_OTHER_RESTRICTED),
        CsvFiles(FILE_OTHER_BENEFITS),
        CsvFiles(FILE_OTHER_CONVERTABLE),
        CsvFiles(FILE_OTHER_NOTIONAL),
        CsvFiles(FILE_OTHER_ENCHANCEMENT),
        CsvFiles(FILE_OTHER_SOLD)
      )
    ),
    (
      SAYE,
      List(
        CsvFiles(FILE_SAYE_GRANTED),
        CsvFiles(FILE_SAYE_RCL),
        CsvFiles(FILE_SAYE_EXERCISED)
      )
    ),
    (
      SIP,
      List(
        CsvFiles(FILE_SIP_AWARDS),
        CsvFiles(FILE_SIP_OUT)
      )
    )
  )

  def getCsvFilesList(scheme: String): List[CsvFiles] =
    CSVFilesList.getOrElse(scheme, List[CsvFiles]())

  def getPageElement(scheme: String, pageId: String, element: String, para: String = "")(implicit
    messages: Messages
  ): String =
    scheme match {
      case SCHEME_CSOP  => messages(pageId + MSG_CSOP + element, para)
      case SCHEME_EMI   => messages(pageId + MSG_EMI + element, para)
      case SCHEME_SAYE  => messages(pageId + MSG_SAYE + element, para)
      case SCHEME_SIP   => messages(pageId + MSG_SIP + element, para)
      case SCHEME_OTHER => messages(pageId + MSG_OTHER + element, para)
      case _            => DEFAULT
    }

  def getPageBackLink(
    schemeId: String,
    pageId: String,
    condition: String = "",
    reportableEvents: String = "2"
  ): String = {
    val backLink: String = schemeId match {
      case SCHEME_CSOP  =>
        pageId match {
          case PAGE_SCHEME_ORGANISER       =>
            reportableEvents match {
              case OPTION_UPLOAD_SPREEDSHEET =>
                condition match {
                  case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
                  case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
                  case _          => DEFAULT
                }
              case OPTION_NIL_RETURN         => routes.ReportableEventsController.reportableEventsPage().toString
            }
          case PAGE_ALT_ACTIVITY           =>
            condition match {
              case OPTION_YES => controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
              case OPTION_NO  => controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage().toString
              case _          => DEFAULT
            }
          case PAGE_ALT_AMENDS             => routes.AltAmendsController.altActivityPage().toString
          case PAGE_GROUP_SUMMARY          =>
            controllers.schemeOrganiser.routes.SchemeOrganiserController.schemeOrganiserSummaryPage().toString
          case PAGE_SUMMARY_DECLARATION    =>
            condition match {
              case OPTION_YES => routes.AltAmendsController.altAmendsPage().toString
              case OPTION_NO  => routes.AltAmendsController.altActivityPage().toString
              case _          => DEFAULT
            }
          case PAGE_CONFIRM_DELETE_COMPANY =>
            controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
          case _                           => DEFAULT
        }
      case SCHEME_EMI   =>
        pageId match {
          case PAGE_SCHEME_ORGANISER       =>
            reportableEvents match {
              case OPTION_UPLOAD_SPREEDSHEET =>
                condition match {
                  case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
                  case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
                  case _          => DEFAULT
                }
              case OPTION_NIL_RETURN         => routes.ReportableEventsController.reportableEventsPage().toString
            }
          case PAGE_GROUP_SUMMARY          =>
            condition match {
              case OPTION_MANUAL =>
                controllers.schemeOrganiser.routes.SchemeOrganiserController.schemeOrganiserSummaryPage().toString
              case _             => DEFAULT
            }
          case PAGE_SUMMARY_DECLARATION    =>
            condition match {
              case OPTION_YES => controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
              case OPTION_NO  => controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage().toString
              case _          => DEFAULT
            }
          case PAGE_CONFIRM_DELETE_COMPANY =>
            controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
          case _                           => DEFAULT
        }
      case SCHEME_SAYE  =>
        pageId match {
          case PAGE_SCHEME_ORGANISER       =>
            reportableEvents match {
              case OPTION_UPLOAD_SPREEDSHEET =>
                condition match {
                  case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
                  case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
                  case _          => DEFAULT
                }
              case OPTION_NIL_RETURN         => routes.ReportableEventsController.reportableEventsPage().toString
            }
          case PAGE_ALT_ACTIVITY           =>
            condition match {
              case OPTION_YES => controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
              case OPTION_NO  => controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage().toString
              case _          => DEFAULT
            }
          case PAGE_ALT_AMENDS             => routes.AltAmendsController.altActivityPage().toString
          case PAGE_GROUP_SUMMARY          =>
            condition match {
              case OPTION_MANUAL =>
                controllers.schemeOrganiser.routes.SchemeOrganiserController.schemeOrganiserSummaryPage().toString
              case _             => DEFAULT
            }
          case PAGE_SUMMARY_DECLARATION    =>
            condition match {
              case OPTION_YES => routes.AltAmendsController.altAmendsPage().toString
              case OPTION_NO  => controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage().toString
              case _          => DEFAULT
            }
          case PAGE_CONFIRM_DELETE_COMPANY =>
            controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
          case _                           => DEFAULT
        }
      case SCHEME_SIP   =>
        pageId match {
          case PAGE_SCHEME_ORGANISER       =>
            reportableEvents match {
              case OPTION_UPLOAD_SPREEDSHEET =>
                condition match {
                  case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
                  case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
                  case _          => DEFAULT
                }
              case OPTION_NIL_RETURN         => routes.ReportableEventsController.reportableEventsPage().toString
            }
          case PAGE_ALT_ACTIVITY           => controllers.trustees.routes.TrusteeSummaryController.trusteeSummaryPage().toString
          case PAGE_ALT_AMENDS             => routes.AltAmendsController.altActivityPage().toString
          case PAGE_GROUP_SUMMARY          =>
            controllers.schemeOrganiser.routes.SchemeOrganiserController.schemeOrganiserSummaryPage().toString
          case PAGE_SUMMARY_DECLARATION    =>
            condition match {
              case OPTION_YES => routes.AltAmendsController.altAmendsPage().toString
              case OPTION_NO  => routes.AltAmendsController.altActivityPage().toString
              case _          => DEFAULT
            }
          case PAGE_TRUSTEE_DETAILS        =>
            condition match {
              case OPTION_YES => controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
              case OPTION_NO  => controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage().toString
              case _          => DEFAULT
            }
          case PAGE_CONFIRM_DELETE_COMPANY =>
            controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
          case _                           => DEFAULT
        }
      case SCHEME_OTHER =>
        pageId match {
          case PAGE_SCHEME_ORGANISER       =>
            reportableEvents match {
              case OPTION_UPLOAD_SPREEDSHEET =>
                condition match {
                  case OPTION_ODS => routes.FileUploadController.uploadFilePage().toString
                  case OPTION_CSV => routes.CheckCsvFilesController.checkCsvFilesPage().toString
                  case _          => DEFAULT
                }
              case OPTION_NIL_RETURN         => routes.ReportableEventsController.reportableEventsPage().toString
            }
          case PAGE_GROUP_SUMMARY          =>
            condition match {
              case OPTION_MANUAL =>
                controllers.schemeOrganiser.routes.SchemeOrganiserController.schemeOrganiserSummaryPage().toString
              case _             => DEFAULT
            }
          case PAGE_SUMMARY_DECLARATION    =>
            condition match {
              case OPTION_YES => controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
              case OPTION_NO  => controllers.subsidiaries.routes.GroupSchemeController.groupSchemePage().toString
              case _          => DEFAULT
            }
          case PAGE_CONFIRM_DELETE_COMPANY =>
            controllers.subsidiaries.routes.GroupSchemeController.groupPlanSummaryPage().toString
          case _                           => DEFAULT
        }
      case _            => DEFAULT
    }
    backLink
  }

}
