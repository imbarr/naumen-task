import org.scalatest.FlatSpec
import util.PhoneParser

class PhoneParserSpec extends FlatSpec {
  "PhoneParser" should "parse valid international phone numbers" in {
    val phones = List(
      "+78005553535",
      "+79225553535",
      "+1-604-555-5555",
      "+39 06 6-9-85",
      "+39 06 698577777",
      "+7 800 5553535 ext. 5"
    )
    for(phone <- phones)
      assert(PhoneParser.parse(phone).isDefined)
  }

  it should "not parse invalid or national phone numbers" in {
    val phones = List(
      "88005553535",
      "a+78005553535",
      "+78005553535a",
      "+7800-flowers",
      "+71005553535"
    )
    for(phone <- phones)
      assert(PhoneParser.parse(phone).isEmpty)
  }
}
