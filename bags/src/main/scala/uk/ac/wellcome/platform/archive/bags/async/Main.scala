package uk.ac.wellcome.platform.archive.bags.async

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.platform.archive.bags.async.services.BagsWorkerService
import uk.ac.wellcome.platform.archive.common.models.StorageManifest
import uk.ac.wellcome.platform.archive.common.services.StorageManifestService
import uk.ac.wellcome.platform.archive.common.storage.StorageManifestVHS
import uk.ac.wellcome.storage.typesafe.{S3Builder, VHSBuilder}
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val storageManifestService = new StorageManifestService(
      s3Client = S3Builder.buildS3Client(config)
    )

    val storageManifestVHS = new StorageManifestVHS(
      underlying = VHSBuilder.buildVHS[StorageManifest, EmptyMetadata](config)
    )

    new BagsWorkerService(
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      storageManifestService = storageManifestService,
      storageManifestVHS = storageManifestVHS,
      progressSnsWriter = SNSBuilder.buildSNSWriter(config)
    )
  }
}
