package uk.ac.wellcome.platform.storage.replica_aggregator

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{EitherValues, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.platform.archive.common.EnrichedBagInformationPayload
import uk.ac.wellcome.platform.archive.common.generators.PayloadGenerators
import uk.ac.wellcome.platform.archive.common.ingests.fixtures.IngestUpdateAssertions
import uk.ac.wellcome.platform.archive.common.ingests.models.InfrequentAccessStorageProvider
import uk.ac.wellcome.platform.archive.common.storage.models.PrimaryStorageLocation
import uk.ac.wellcome.platform.storage.replica_aggregator.fixtures.ReplicaAggregatorFixtures
import uk.ac.wellcome.platform.storage.replica_aggregator.models.{
  AggregatorInternalRecord,
  ReplicaPath
}
import uk.ac.wellcome.storage.Version
import uk.ac.wellcome.storage.store.memory.MemoryVersionedStore

class ReplicaAggregatorFeatureTest
    extends FunSpec
    with Matchers
    with ReplicaAggregatorFixtures
    with IngestUpdateAssertions
    with PayloadGenerators
    with Eventually
    with EitherValues
    with IntegrationPatience {

  it("completes after a single primary replica") {
    withLocalSqsQueue { queue =>
      val ingests = new MemoryMessageSender()
      val outgoing = new MemoryMessageSender()

      val payload = createEnrichedBagInformationPayload
      val versionedStore =
        MemoryVersionedStore[ReplicaPath, AggregatorInternalRecord](Map.empty)

      withReplicaAggregatorWorker(
        queue = queue,
        versionedStore = versionedStore,
        ingests = ingests,
        outgoing = outgoing,
        stepName = "aggregating replicas"
      ) { _ =>
        sendNotificationToSQS(queue, payload)

        eventually {
          assertTopicReceivesIngestEvents(
            ingestId = payload.ingestId,
            ingests = ingests,
            expectedDescriptions = Seq(
              "Aggregating replicas succeeded"
            )
          )

          val expectedReplicaPath =
            ReplicaPath(payload.bagRootLocation.path)

          val stored =
            versionedStore.get(id = Version(expectedReplicaPath, 0)).right.value

          stored.identifiedT.location shouldBe Some(
            PrimaryStorageLocation(
              provider = InfrequentAccessStorageProvider,
              prefix = payload.bagRootLocation.asPrefix
            )
          )

          stored.identifiedT.replicas shouldBe empty

          outgoing.getMessages[EnrichedBagInformationPayload] shouldBe Seq(
            payload
          )
        }
      }
    }
  }
}
