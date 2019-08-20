package uk.ac.wellcome.platform.storage.replica_aggregator.models

import java.time.Instant

import uk.ac.wellcome.platform.archive.common.EnrichedBagInformationPayload
import uk.ac.wellcome.platform.archive.common.ingests.models.{
  InfrequentAccessStorageProvider,
  IngestID
}
import uk.ac.wellcome.platform.archive.common.storage.models.{
  PrimaryStorageLocation,
  StorageLocation
}

case class ReplicaResult(
  ingestId: IngestID,
  storageLocation: StorageLocation,
  timestamp: Instant
)

case object ReplicaResult {
  def apply(payload: EnrichedBagInformationPayload): ReplicaResult =
    ReplicaResult(
      ingestId = payload.ingestId,
      storageLocation = PrimaryStorageLocation(
        provider = InfrequentAccessStorageProvider,
        prefix = payload.bagRootLocation.asPrefix
      ),
      timestamp = Instant.now()
    )
}
