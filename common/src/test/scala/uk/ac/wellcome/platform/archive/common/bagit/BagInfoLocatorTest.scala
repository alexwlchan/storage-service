package uk.ac.wellcome.platform.archive.common.bagit

import java.io.FileNotFoundException

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archive.common.bagit.services.BagInfoLocator

import scala.util.Success

class BagInfoLocatorTest extends FunSpec with Matchers {
  describe("locateBagInfo") {
    it("detects a bag-info.txt file") {
      val filenames =
        Seq("/foo/README.txt", "/foo/data", "/foo/bag-info.txt").toIterator
      BagInfoLocator.locateBagInfo(filenames) shouldBe Success(
        "/foo/bag-info.txt")
    }

    it("detects a bag-info.txt file without a leading slash") {
      val filenames =
        Seq("/foo/README.txt", "/foo/data", "bag-info.txt").toIterator
      BagInfoLocator.locateBagInfo(filenames) shouldBe Success("bag-info.txt")
    }

    it("throws an exception if it can't find a bag-info.txt file") {
      val filenames = Seq("/foo/README.txt", "/foo/data").toIterator

      val result = BagInfoLocator.locateBagInfo(filenames)
      result.isFailure shouldBe true
      result.failed.get shouldBe a[FileNotFoundException]
      result.failed.get.getMessage shouldBe "No bag-info.txt file found!"
    }

    it("throws an exception if it finds multiple bag-info.txt files") {
      val filenames = Seq("/foo/bag-info.txt", "/bar/bag-info.txt")

      val result = BagInfoLocator.locateBagInfo(filenames.toIterator)
      result.isFailure shouldBe true
      result.failed.get shouldBe a[IllegalArgumentException]
      result.failed.get.getMessage shouldBe s"Multiple bag-info.txt files found, only wanted one: ${filenames
        .mkString(", ")}"
    }
  }

  describe("bagPathFrom") {
    describe("bagPathFromBagInfoPath") {
      it("returns an empty string if there is no enclosing bag directory") {
        BagInfoLocator.bagPathFrom(bagInfoPath = "bag-info.txt") shouldBe ""
      }

      it("finds the bag path when in a parent directory") {
        BagInfoLocator.bagPathFrom(bagInfoPath = "bag/bag-info.txt") shouldBe "bag/"
      }

      it("ignores bag-info.txt not at the end") {
        BagInfoLocator.bagPathFrom(
          bagInfoPath = "bag-info.txt/bag/bag-info.txt") shouldBe "bag-info.txt/bag/"
      }

      it("preserves preceding slashes") {
        BagInfoLocator.bagPathFrom(bagInfoPath = "/bag/bag-info.txt") shouldBe "/bag/"
      }

      it("preserves preceding slashes even if bag-info.txt is at the root") {
        BagInfoLocator.bagPathFrom(bagInfoPath = "/bag-info.txt") shouldBe "/"
      }
    }
  }
}
