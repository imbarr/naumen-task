package util

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

import scala.util.Try

object PhoneParser {
  private val unknownRegion = "ZZ"
  private val parser = PhoneNumberUtil.getInstance()

  def parse(phone: String): Option[PhoneNumber] =
    Try {
      val result = parser.parse(phone, unknownRegion)
      require(parser.isValidNumber(result))
      result
    }.toOption

  def format(phone: PhoneNumber): String =
    parser.format(phone, PhoneNumberFormat.INTERNATIONAL)
}
