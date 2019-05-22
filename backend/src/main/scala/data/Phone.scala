package data

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

import scala.util.Try

object Phone {
  private val unknownRegion = "ZZ"
  private val delimiters = Set('-', ' ')
  private val parser = PhoneNumberUtil.getInstance()

  def fromString(phone: String): Either[Throwable, Phone] =
    Try {
      val result = parser.parse(phone, unknownRegion)
      require(parser.isValidNumber(result))
      // It is not likely that we should consider phone
      // number extention, they are used very rarely now
      require(!result.hasExtension)
      new Phone(result)
    }.toEither

  def withoutDelimiters(string: String): String =
    string.filterNot(delimiters.contains)
}

case class Phone private(phoneNumber: PhoneNumber) {
  def withoutDelimiters: String =
    Phone.withoutDelimiters(formatted)

  def formatted: String =
    Phone.parser.format(phoneNumber, PhoneNumberFormat.INTERNATIONAL)
}
