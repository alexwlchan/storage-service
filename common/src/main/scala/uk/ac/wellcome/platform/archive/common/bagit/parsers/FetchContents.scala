package uk.ac.wellcome.platform.archive.common.bagit.parsers

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URI

import uk.ac.wellcome.platform.archive.common.bagit.models.FetchEntry

import scala.util.matching.Regex

/** Read/write the contents of a Fetch File as defined by RFC 8493 § 2.2.3.
  *
  * Relevant notes:
  *
  *   - Each line of a fetch file MUST be of the form
  *
  *         url length filepath
  *
  *   - `url` must be an absolute URI, and whitespace characters must be
  *     percent encoded
  *   - `length` is the number of octets in the file, or "-" if unspecified
  *   - `filename` is the path to the file.  Line break characters (LR, CF, LRCF)
  *     and *only* those characters must be percent-encoded.
  *
  */
object FetchContents {

  val FETCH_LINE_REGEX: Regex = new Regex(
    "(.*)[ \t]+(\\d*|-)[ \t]+(.*)",
    "url",
    "length",
    "filepath"
  )

  def read(is: InputStream): Seq[FetchEntry] = {
    val bufferedReader = new BufferedReader(new InputStreamReader(is))

    Iterator
      .continually(bufferedReader.readLine())
      .takeWhile { _ != null }
      .filterNot { _.trim.isEmpty }
      .map { line: String =>
        val m = FETCH_LINE_REGEX.findFirstMatchIn(line).get

        FetchEntry(
          url = new URI(m.group("url")),
          length = decodeLength(m.group("length")),
          filepath = decodeFilepath(m.group("filepath"))
        )
      }
      .toSeq
  }

  def write(entries: Seq[FetchEntry]): String =
    entries
      .map { e =>
        s"${e.url} ${encodeLength(e.length)} ${encodeFilepath(e.filepath)}"
      }
      .mkString("\n")

  private def encodeLength(length: Option[Int]): String =
    length match {
      case Some(i) => i.toString
      case None    => "-"
    }

  private def decodeLength(ls: String): Option[Int] =
    if (ls == "-") None else Some(ls.toInt)

  private def encodeFilepath(path: String): String =
    path.replaceAll("\n", "%0A").replaceAll("\r", "%0D")

  private def decodeFilepath(path: String): String =
    path.replaceAll("%0A", "\n").replaceAll("%0D", "\r")
}
