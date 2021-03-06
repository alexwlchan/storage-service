package uk.ac.wellcome.platform.archive.indexer

import com.sksamuel.elastic4s.ElasticDsl.{properties, textField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import io.circe.Decoder
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.messaging.worker.models.{
  NonDeterministicFailure,
  Successful
}
import uk.ac.wellcome.platform.archive.indexer.fixtures.IndexerFixtures

abstract class IndexerWorkerTestCases[SourceT, T, IndexedT](
  implicit
  decoderT: Decoder[SourceT],
  decoderIT: Decoder[IndexedT]
) extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexerFixtures[SourceT, T, IndexedT] {

  // TODO: Cover the code path where "load" is called
  // We should have tests to test failure modes in load
  // If this code is shared with the catalogue we should add those.

  val mapping: MappingDefinition

  def createT: (SourceT, String)

  def convertToIndexedT(sourceT: SourceT): IndexedT

  protected val badMapping: MappingDefinition = properties(
    Seq(textField("name"))
  )

  it("processes a single message") {
    val (t, id) = createT
    withLocalElasticsearchIndex(mapping) { index =>
      withIndexerWorker(index) { indexerWorker =>
        whenReady(indexerWorker.process(t)) { result =>
          result shouldBe a[Successful[_]]
          val expectedIndexedT = convertToIndexedT(t)
          val actualIndexedT = getT[IndexedT](index, id)

          actualIndexedT shouldBe expectedIndexedT
        }
      }

      val expectedIndexedT = convertToIndexedT(t)
      val actualIndexedT = getT[IndexedT](index, id)

      actualIndexedT shouldBe expectedIndexedT
    }
  }

  it("fails if it cannot index T") {
    val (t, _) = createT

    withLocalElasticsearchIndex(badMapping) { index =>
      withIndexerWorker(index) { worker =>
        whenReady(worker.process(t)) {
          _ shouldBe a[NonDeterministicFailure[_]]
        }
      }
    }
  }
}
