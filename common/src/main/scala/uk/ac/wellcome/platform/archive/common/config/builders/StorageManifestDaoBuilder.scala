package uk.ac.wellcome.platform.archive.common.config.builders

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.Config
import org.scanamo.auto._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.streaming.Codec._
import uk.ac.wellcome.platform.archive.common.bagit.models.BagId
import uk.ac.wellcome.platform.archive.common.storage.models.StorageManifest
import uk.ac.wellcome.platform.archive.common.storage.services.StorageManifestDao
import uk.ac.wellcome.storage.{ObjectLocation, ObjectLocationPrefix, Version}
import uk.ac.wellcome.storage.store.HybridIndexedStoreEntry
import uk.ac.wellcome.storage.store.dynamo.{DynamoHashRangeStore, DynamoHybridStoreWithMaxima, DynamoVersionedHybridStore}
import uk.ac.wellcome.storage.store.s3.{S3StreamStore, S3TypedStore}
import uk.ac.wellcome.storage.typesafe.{DynamoBuilder, S3Builder}

object StorageManifestDaoBuilder {
  def build(config: Config): StorageManifestDao = {

    // TODO: This should be a builder class
    implicit val dynamoClient: AmazonDynamoDB =
      DynamoBuilder.buildDynamoClient(config)

    implicit val s3Client: AmazonS3 =
      S3Builder.buildS3Client(config)

    implicit val streamStore: S3StreamStore = new S3StreamStore()
    implicit val typedStore: S3TypedStore[StorageManifest] = new S3TypedStore[StorageManifest]()

    implicit val indexedStore:
      DynamoHashRangeStore[BagId,
                           Int,
                           HybridIndexedStoreEntry[Version[BagId, Int],
                                                   ObjectLocation,
                                                   Map[String, String]]] = new DynamoHashRangeStore[
      BagId,
      Int,
      HybridIndexedStoreEntry[Version[BagId, Int], ObjectLocation, Map[String, String]]](
      config = DynamoBuilder.buildDynamoConfig(config, namespace = "vhs")
    )

    val vhs =
      new DynamoVersionedHybridStore[BagId, Int, StorageManifest, Map[String, String]](
        store = new DynamoHybridStoreWithMaxima[BagId, Int, StorageManifest, Map[String, String]](
          prefix = ObjectLocationPrefix(
            namespace = S3Builder.buildS3Config(config, namespace = "vhs").bucketName,
            path = ""
          )
        )
      )

    new StorageManifestDao(vhs)
  }
}
