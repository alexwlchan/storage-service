package uk.ac.wellcome.platform.archive.common.verify

sealed trait VerifiedLocation

case class VerifiedSuccess(location: VerifiableLocation)
    extends VerifiedLocation
case class VerifiedFailure(location: VerifiableLocation, e: Throwable)
    extends VerifiedLocation {
  override def toString: String = s"Failed @ ${location.uri}: ${e.getMessage}"
}
