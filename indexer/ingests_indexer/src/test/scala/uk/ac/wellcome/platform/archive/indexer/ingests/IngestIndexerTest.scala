package uk.ac.wellcome.platform.archive.indexer.ingests

import java.util.UUID

import com.sksamuel.elastic4s.ElasticDsl.matchAllQuery
import com.sksamuel.elastic4s.http.JavaClientExceptionWrapper
import io.circe.Json
import org.scalatest.{EitherValues, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.bagit.models.ExternalIdentifier
import uk.ac.wellcome.platform.archive.common.generators.IngestGenerators
import uk.ac.wellcome.platform.archive.indexer.elasticsearch.ElasticClientFactory
import uk.ac.wellcome.platform.archive.indexer.fixtures.ElasticsearchFixtures

import scala.concurrent.ExecutionContext.Implicits.global

class IngestIndexerTest
  extends FunSpec
    with Matchers
    with EitherValues
    with IngestGenerators
    with ElasticsearchFixtures {

  it("indexes a single ingest") {
    withLocalElasticsearchIndex(IngestsIndexConfig.mapping) { index =>
      val ingestsIndexer = new IngestIndexer(elasticClient, index = index)

      val ingest = createIngest

      whenReady(ingestsIndexer.index(Seq(ingest))) { result =>
        result.right.value shouldBe Seq(ingest)

        val storedIngest =
          getT[Json](index, id = ingest.id.toString)
            .as[Map[String, Json]]
            .right.value

        val storedIngestId = UUID.fromString(storedIngest("id").asString.get)
        storedIngestId shouldBe ingest.id.underlying

        val storedExternalIdentifier = ExternalIdentifier(
          storedIngest("bag")
            .asObject.get
            .toMap("info").asObject.get
            .toMap("externalIdentifier").asString.get
        )
        storedExternalIdentifier shouldBe ingest.externalIdentifier
      }
    }
  }

  it("indexes multiple ingests") {
    withLocalElasticsearchIndex(IngestsIndexConfig.mapping) { index =>
      val ingestsIndexer = new IngestIndexer(elasticClient, index = index)

      val ingestCount = 10
      val ingests = (1 to ingestCount).map { _ => createIngest }

      whenReady(ingestsIndexer.index(ingests)) { result =>
        result.right.value shouldBe ingests

        eventually {
          val storedIngests = searchT[Json](index, query = matchAllQuery())

          storedIngests should have size ingestCount

          val storedIds =
            storedIngests
              .map { _.as[Map[String, Json]].right.value }
              .map { _("id").asString.get }

          storedIds shouldBe ingests.map { _.id.toString }
        }
      }
    }
  }

  it("fails if Elasticsearch doesn't respond") {
    val badClient = ElasticClientFactory.create(
      hostname = esHost,
      port = esPort + 1,
      protocol = "http",
      username = "elastic",
      password = "changeme"
    )

    val index = createIndex
    val ingestsIndexer = new IngestIndexer(badClient, index = index)

    val ingest = createIngest

    whenReady(ingestsIndexer.index(Seq(ingest)).failed) {
      _ shouldBe a[JavaClientExceptionWrapper]
    }
  }
}
