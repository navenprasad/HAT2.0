/*
 * Copyright (C) 2017 HAT Data Exchange Ltd
 * SPDX-License-Identifier: AGPL-3.0
 *
 * This file is part of the Hub of All Things project (HAT).
 *
 * HAT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of
 * the License.
 *
 * HAT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General
 * Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Written by Andrius Aucinas <andrius.aucinas@hatdex.org>
 * 5 / 2017
 */

package org.hatdex.hat.api.service.monitoring

import javax.inject.Inject

import akka.actor.ActorRef
import akka.event.{ EventBus, SubchannelClassification }
import akka.util.Subclassification
import com.google.inject.Singleton
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import org.hatdex.hat.api.models._
import org.hatdex.hat.authentication.{ HatApiAuthEnvironment, WithRole }
import org.hatdex.hat.authentication.models.HatUser
import org.hatdex.hat.dal.ModelTranslation
import org.hatdex.hat.resourceManagement.HatServer
import org.joda.time.DateTime

import scala.util.{ Success, Try }

/**
 * Publishes the payload of the MsgEnvelope when the topic of the
 * MsgEnvelope equals the String specified when subscribing.
 */
@Singleton
class HatDataEventBus extends EventBus with SubchannelClassification {
  import HatDataEventBus._

  type Event = HatDataEvent
  type Classifier = Class[_ <: HatDataEvent]
  type Subscriber = ActorRef

  protected def compareSubscribers(a: Subscriber, b: Subscriber) = a compareTo b

  /**
   * The logic to form sub-class hierarchy
   */
  override protected implicit val subclassification = new Subclassification[Classifier] {
    def isEqual(x: Classifier, y: Classifier): Boolean = x == y
    def isSubclass(x: Classifier, y: Classifier): Boolean = y.isAssignableFrom(x)
  }

  /**
   * Publishes the given Event to the given Subscriber.
   *
   * @param event The Event to publish.
   * @param subscriber The Subscriber to which the Event should be published.
   */
  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  /**
   * Returns the Classifier associated with the given Event.
   *
   * @param event The event for which the Classifier should be returned.
   * @return The Classifier for the given Event.
   */
  override protected def classify(event: Event): Classifier = event.getClass
}

class HatDataEventDispatcher @Inject() (dataEventBus: HatDataEventBus) {
  import scala.language.implicitConversions
  implicit def userModelTranslation(user: HatUser): User = ModelTranslation.fromInternalModel(user)

  def dispatchEventDataCreated(message: String)(implicit request: SecuredRequest[HatApiAuthEnvironment, _]): PartialFunction[Try[Seq[EndpointData]], Unit] = {
    case Success(saved) =>
      dataEventBus.publish(HatDataEventBus.DataCreatedEvent(
        request.dynamicEnvironment.hatName,
        request.identity,
        DateTime.now(),
        message, saved))
  }

  def dispatchEventDataDebit(operation: DataDebitOperations.DataDebitOperation)(implicit request: SecuredRequest[HatApiAuthEnvironment, _]): PartialFunction[Try[RichDataDebit], Unit] = {
    case Success(saved) =>
      dataEventBus.publish(HatDataEventBus.DataDebitEvent(
        request.dynamicEnvironment.hatName,
        request.identity,
        DateTime.now(),
        operation.toString, saved, operation))
  }

  def dispatchEventMaybeDataDebit(operation: DataDebitOperations.DataDebitOperation)(implicit request: SecuredRequest[HatApiAuthEnvironment, _]): PartialFunction[Try[Option[RichDataDebit]], Unit] = {
    case Success(Some(saved)) =>
      dataEventBus.publish(HatDataEventBus.DataDebitEvent(
        request.dynamicEnvironment.hatName,
        request.identity,
        DateTime.now(),
        operation.toString, saved, operation))
  }

  def dispatchEventDataDebitValues(debit: RichDataDebit)(implicit request: SecuredRequest[HatApiAuthEnvironment, _]): PartialFunction[Try[Map[String, Seq[EndpointData]]], Unit] = {
    case Success(data) => dataEventBus.publish(HatDataEventBus.DataRetrievedEvent(
      request.dynamicEnvironment.hatName,
      request.identity,
      DateTime.now(),
      DataDebitOperations.GetValues().toString, debit, data.values.flatten.toSeq))
  }
}

object HatDataEventBus {
  sealed trait HatDataEvent {
    val hat: String
    val user: User
    val time: DateTime
    val logEntry: String
  }

  case class DataCreatedEvent(
    hat: String,
    user: User,
    time: DateTime,
    logEntry: String,
    data: Seq[EndpointData]) extends HatDataEvent

  case class DataDebitEvent(
    hat: String,
    user: User,
    time: DateTime,
    logEntry: String,
    dataDebit: RichDataDebit,
    operation: DataDebitOperations.DataDebitOperation) extends HatDataEvent

  case class DataRetrievedEvent(
    hat: String,
    user: User,
    time: DateTime,
    logEntry: String,
    dataDebit: RichDataDebit,
    data: Seq[EndpointData]) extends HatDataEvent

  case class HatDataSubscriber(hat: HatServer)
}