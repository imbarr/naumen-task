package data

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object Implicits {
  implicit val phoneEncoder = new Encoder[Phone] {
    override final def apply(phone: Phone): Json =
      Json.fromString(phone.formatted)
  }

  implicit val phoneDecoder = new Decoder[Phone] {
    override final def apply(cursor: HCursor): Decoder.Result[Phone] = {
      for {
        string <- cursor.as[String]
        resultEither = Phone.fromString(string)
        result <- resultEither.left.map(DecodingFailure.fromThrowable(_, cursor.history))
      } yield result
    }
  }

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
