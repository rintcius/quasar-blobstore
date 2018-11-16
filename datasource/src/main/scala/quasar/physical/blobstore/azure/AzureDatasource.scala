/*
 * Copyright 2014–2018 SlamData Inc.
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

package quasar.physical.blobstore.azure

import quasar.api.datasource.DatasourceType
import quasar.blobstore.ResourceType
import quasar.blobstore.azure.{Azure, AzureBlobstore, AzureConfig, MaxQueueSize}
import quasar.connector.MonadResourceErr
import quasar.connector.ParsableType.JsonVariant
import quasar.physical.blobstore.BlobstoreDatasource

import cats.Applicative
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.syntax.functor._
import eu.timepit.refined.auto._
import fs2.RaiseThrowable

class AzureDatasource[F[_]: Applicative: MonadResourceErr: RaiseThrowable](
  azureBlobstore: AzureBlobstore[F],
  jsonVariant: JsonVariant)
  extends BlobstoreDatasource[F](AzureDatasource.dsType, jsonVariant, azureBlobstore)

object AzureDatasource {
  val dsType: DatasourceType = DatasourceType("azure", 1L)

  def mk[F[_]: ConcurrentEffect: ContextShift: MonadResourceErr](cfg: AzureConfig): F[AzureDatasource[F]] =
    Azure.mkContainerUrl[F](cfg)
      .map(c => new AzureDatasource[F](
        new AzureBlobstore(c, cfg.maxQueueSize.getOrElse(MaxQueueSize.default)),
        toJsonVariant(cfg.resourceType)))

  private def toJsonVariant(resourceType: ResourceType): JsonVariant =
    resourceType match {
      case ResourceType.Json => JsonVariant.ArrayWrapped
      case ResourceType.LdJson => JsonVariant.LineDelimited
    }
}
