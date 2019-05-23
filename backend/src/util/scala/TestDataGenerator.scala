import java.util.Set

import scala.collection.JavaConverters._
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import data.Phone

object TestDataGenerator extends App {
  val util = PhoneNumberUtil.getInstance()
  val field = util.getClass.getDeclaredField("supportedRegions")
  field.setAccessible(true)
  val obj = field.get(util)
  val supportedRegions = obj.asInstanceOf[Set[String]].asScala
  println("-- This code was generated with /src/util/scala/TestDataGenerator")
  for ((region, index) <- supportedRegions.toSeq.zipWithIndex) {
    val number = util.getExampleNumber(region)
    val numberString = Phone.withoutDelimiters(util.format(number, PhoneNumberFormat.INTERNATIONAL))
    val query = s"""insert into phoneNumbers(name, phone) values ('Test $index', '$numberString')"""
    println(query)
  }
}
