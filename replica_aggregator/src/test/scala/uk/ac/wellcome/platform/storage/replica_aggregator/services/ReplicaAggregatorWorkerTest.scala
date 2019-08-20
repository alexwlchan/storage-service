package uk.ac.wellcome.platform.storage.replica_aggregator.services

import org.scalatest.{FunSpec, Matchers, TryValues}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.platform.archive.common.EnrichedBagInformationPayload
import uk.ac.wellcome.platform.archive.common.generators.PayloadGenerators
import uk.ac.wellcome.platform.archive.common.ingests.fixtures.IngestUpdateAssertions
import uk.ac.wellcome.platform.archive.common.ingests.models.{
  InfrequentAccessStorageProvider,
  Ingest
}
import uk.ac.wellcome.platform.archive.common.storage.models.{
  IngestFailed,
  IngestStepSucceeded,
  PrimaryStorageLocation
}
import uk.ac.wellcome.platform.storage.replica_aggregator.fixtures.ReplicaAggregatorFixtures
import uk.ac.wellcome.platform.storage.replica_aggregator.models._
import uk.ac.wellcome.storage.maxima.memory.MemoryMaxima
import uk.ac.wellcome.storage.{UpdateWriteError, Version}
import uk.ac.wellcome.storage.store.memory.{MemoryStore, MemoryVersionedStore}

class ReplicaAggregatorWorkerTest
    extends FunSpec
    with Matchers
    with PayloadGenerators
    with IngestUpdateAssertions
    with ReplicaAggregatorFixtures
    with TryValues {

  describe("if there are enough replicas") {
    val ingests = new MemoryMessageSender()
    val outgoing = new MemoryMessageSender()

    val payload = createEnrichedBagInformationPayload

    val result =
      withReplicaAggregatorWorker(
        ingests = ingests,
        outgoing = outgoing,
        stepName = "aggregating replicas"
      ) {
        _.processMessage(payload).success.value
      }

    it("returns a ReplicationAggregationComplete") {
      result shouldBe a[IngestStepSucceeded[_]]

      result.summary shouldBe a[ReplicationAggregationComplete]

      val completeAggregation =
        result.summary.asInstanceOf[ReplicationAggregationComplete]
      completeAggregation.replicaPath shouldBe ReplicaPath(
        payload.bagRootLocation.path
      )
      completeAggregation.knownReplicas shouldBe KnownReplicas(
        location = PrimaryStorageLocation(
          provider = InfrequentAccessStorageProvider,
          prefix = payload.bagRootLocation.asPrefix
        ),
        replicas = List.empty
      )
    }

    it("sends an outgoing message") {
      outgoing.getMessages[EnrichedBagInformationPayload] shouldBe Seq(payload)
    }

    it("updates the ingests monitor") {
      assertTopicReceivesIngestEvents(
        ingestId = payload.ingestId,
        ingests = ingests,
        expectedDescriptions = Seq(
          "Aggregating replicas succeeded"
        )
      )
    }
  }

  describe("if there are not enough replicas") {
    val ingests = new MemoryMessageSender()
    val outgoing = new MemoryMessageSender()

    val payload = createEnrichedBagInformationPayload

    val result =
      withReplicaAggregatorWorker(
        ingests = ingests,
        outgoing = outgoing,
        stepName = "aggregating replicas",
        expectedReplicaCount = 3
      ) {
        _.processMessage(payload).success.value
      }

    it("returns a ReplicationAggregationIncomplete") {
      result shouldBe a[IngestStepSucceeded[_]]

      result.summary shouldBe a[ReplicationAggregationIncomplete]

      val incompleteAggregation =
        result.summary.asInstanceOf[ReplicationAggregationIncomplete]
      incompleteAggregation.replicaPath shouldBe ReplicaPath(
        payload.bagRootLocation.path
      )
      incompleteAggregation.aggregatorRecord shouldBe AggregatorInternalRecord(
        location = Some(
          PrimaryStorageLocation(
            provider = InfrequentAccessStorageProvider,
            prefix = payload.bagRootLocation.asPrefix
          )
        ),
        replicas = List.empty
      )
      incompleteAggregation.counterError shouldBe NotEnoughReplicas(
        expected = 3,
        actual = 1
      )
    }

    it("does not send an outgoing message") {
      outgoing.getMessages[EnrichedBagInformationPayload] shouldBe empty
    }

    it("updates the ingests monitor") {
      assertTopicReceivesIngestEvents(
        ingestId = payload.ingestId,
        ingests = ingests,
        expectedDescriptions = Seq(
          "Aggregating replicas succeeded"
        )
      )
    }
  }

  describe("if there's an error in the replica aggregator") {
    val throwable = new Throwable("BOOM!")

    val brokenStore =
      new MemoryVersionedStore[ReplicaPath, AggregatorInternalRecord](
        store =
          new MemoryStore[Version[ReplicaPath, Int], AggregatorInternalRecord](
            initialEntries = Map.empty
          ) with MemoryMaxima[ReplicaPath, AggregatorInternalRecord]
      ) {
        override def upsert(id: ReplicaPath)(
          t: AggregatorInternalRecord
        )(
          f: AggregatorInternalRecord => AggregatorInternalRecord
        ): UpdateEither =
          Left(UpdateWriteError(throwable))
      }

    val ingests = new MemoryMessageSender()
    val outgoing = new MemoryMessageSender()

    val payload = createEnrichedBagInformationPayload

    val result =
      withReplicaAggregatorWorker(
        ingests = ingests,
        outgoing = outgoing,
        versionedStore = brokenStore,
        stepName = "aggregating replicas"
      ) {
        _.processMessage(payload).success.value
      }

    it("returns a ReplicationAggregationFailed") {
      result shouldBe a[IngestFailed[_]]

      result.summary shouldBe a[ReplicationAggregationFailed]

      val failure = result.summary.asInstanceOf[ReplicationAggregationFailed]
      failure.replicaPath shouldBe ReplicaPath(payload.bagRootLocation.path)
      failure.e shouldBe throwable
    }

    it("does not send an outgoing message") {
      outgoing.getMessages[EnrichedBagInformationPayload] shouldBe empty
    }

    it("sends an IngestFailed to the monitor") {
      assertTopicReceivesIngestStatus(
        ingestId = payload.ingestId,
        ingests = ingests,
        status = Ingest.Failed
      ) {
        _.map { _.description } shouldBe Seq("Aggregating replicas failed")
      }
    }
  }
}
