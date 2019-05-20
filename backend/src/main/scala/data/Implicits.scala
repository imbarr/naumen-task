package data

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object Implicits {
  implicit val bookEntryWithIdEncoder = deriveEncoder[BookEntryWithId]
  implicit val bookEntryEncoder = deriveEncoder[BookEntry]
  implicit val nameWrapperEncoder = deriveEncoder[NameWrapper]
  implicit val phoneNumberWrapperEncoder = deriveEncoder[PhoneNumberWrapper]
  implicit val taskStatusEncoder = deriveEncoder[TaskStatus]

  implicit val bookEntryWithIdDecoder = deriveDecoder[BookEntryWithId]
  implicit val bookEntryDecoder = deriveDecoder[BookEntry]
  implicit val nameWrapperDecoder = deriveDecoder[NameWrapper]
  implicit val phoneNumberWrapperDecoder = deriveDecoder[PhoneNumberWrapper]
  implicit val taskStatusDecoder = deriveDecoder[TaskStatus]
}
