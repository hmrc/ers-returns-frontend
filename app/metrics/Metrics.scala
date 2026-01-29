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

package metrics

import com.codahale.metrics.MetricRegistry

import java.util.concurrent.TimeUnit

trait Metrics {
  val registry = new MetricRegistry

  def ersConnector(diff: Long, unit: TimeUnit): Unit = registry.timer("validator-connector").update(diff, unit)

  def submitReturnToBackend(diff: Long, unit: TimeUnit): Unit =
    registry.timer("submit-to-returns-connector").update(diff, unit)

}
