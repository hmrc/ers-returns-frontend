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

package utils

import config.ApplicationConfig
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern.after
import play.api.Logging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait Retryable extends Logging {
  val appConfig: ApplicationConfig

  case class LoopException[A](retryNumber: Int, finalFutureData: Option[A])
      extends Exception(s"Failed to meet predicate after retrying $retryNumber times.")

  implicit class RetryCache[A](f: => Future[A]) {
    def withRetry(
      maxTimes: Int
    )(pToBreakLoop: A => Boolean)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[A] = {
      val delay: FiniteDuration = appConfig.retryDelay
      val scheduler: Scheduler  = actorSystem.getScheduler
      def loop(count: Int = 0, previous: Option[A] = None): Future[A] = {
        logger.info(s"Retrying call x$count")
        if (count < maxTimes) {
          f.flatMap { data =>
            if (pToBreakLoop(data)) {
              Future.successful(data)
            } else {
              after(delay, scheduler)(loop(count + 1, Some(data)))
            }
          }
        } else {
          throw LoopException(count, previous)
        }
      }
      loop()
    }
  }
}
