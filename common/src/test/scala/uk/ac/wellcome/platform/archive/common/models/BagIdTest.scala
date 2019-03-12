package uk.ac.wellcome.platform.archive.common.models
import org.scalatest.FunSpec
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.platform.archive.common.bagit.models.{BagId, ExternalIdentifier}
import uk.ac.wellcome.platform.archive.common.storage.models.StorageSpace

class BagIdTest extends FunSpec with JsonAssertions {
  it("serialises space and external identifier as strings") {
    val bagId =
      BagId(StorageSpace("digitised"), ExternalIdentifier("b1234567x"))
    val expectedJson =
      s"""
         |{
         |  "space": "digitised",
         |  "externalIdentifier": "b1234567x"
         |}
       """.stripMargin
    assertJsonStringsAreEqual(toJson(bagId).get, expectedJson)
  }

}
