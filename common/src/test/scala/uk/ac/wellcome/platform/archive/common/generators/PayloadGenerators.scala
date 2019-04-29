package uk.ac.wellcome.platform.archive.common.generators

import uk.ac.wellcome.platform.archive.common.{
  BagInformationPayload,
  IngestID,
  ObjectLocationPayload
}
import uk.ac.wellcome.platform.archive.common.bagit.models.ExternalIdentifier
import uk.ac.wellcome.platform.archive.common.storage.models.StorageSpace
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3

trait PayloadGenerators extends StorageSpaceGenerators with S3 {
  def createObjectLocationPayloadWith(
    objectLocation: ObjectLocation = createObjectLocation,
    storageSpace: StorageSpace = createStorageSpace): ObjectLocationPayload =
    ObjectLocationPayload(
      ingestId = createIngestID,
      storageSpace = storageSpace,
      objectLocation = objectLocation
    )

  def createBagInformationPayloadWith(ingestId: IngestID = createIngestID,
                                      bagRootLocation: ObjectLocation,
                                      storageSpace: StorageSpace,
                                      externalIdentifier: ExternalIdentifier,
                                      version: Int = 1): BagInformationPayload =
    BagInformationPayload(
      ingestId = ingestId,
      storageSpace = storageSpace,
      bagRootLocation = bagRootLocation,
      externalIdentifier = externalIdentifier,
      version = version
    )

  def createObjectLocationPayload: ObjectLocationPayload =
    createObjectLocationPayloadWith()
}
