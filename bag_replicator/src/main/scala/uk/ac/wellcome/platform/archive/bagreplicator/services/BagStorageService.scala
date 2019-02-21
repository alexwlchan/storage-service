package uk.ac.wellcome.platform.archive.bagreplicator.services

import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.bagreplicator.storage.S3PrefixCopier
import uk.ac.wellcome.platform.archive.common.models.bagit.BagLocation

import scala.concurrent.{ExecutionContext, Future}

class BagStorageService(s3PrefixCopier: S3PrefixCopier)(
  implicit ec: ExecutionContext)
    extends Logging {
  def duplicateBag(
    sourceBagLocation: BagLocation,
    destinationConfig: ReplicatorDestinationConfig
  ): Future[Either[Throwable, BagLocation]] = {
    debug(s"duplicating bag from $sourceBagLocation to $destinationConfig")

    val dstBagLocation = sourceBagLocation.copy(
      storageNamespace = destinationConfig.namespace,
      storagePrefix = destinationConfig.rootPath
    )

    val future: Future[Unit] = s3PrefixCopier.copyObjects(
      srcLocationPrefix = sourceBagLocation.objectLocation,
      dstLocationPrefix = dstBagLocation.objectLocation
    )

    future
      .map { _ =>
        Right(dstBagLocation)
      }
      .recover {
        case err: Throwable => Left(err)
      }
  }
}