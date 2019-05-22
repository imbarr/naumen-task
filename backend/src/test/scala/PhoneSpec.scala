import data.Phone
import org.scalatest.FlatSpec

class PhoneSpec extends FlatSpec {
  val valid = List(
    "+78005553535",
    "+79225553535",
    "+1604-555-5555",
    "+39 06 6-9-85",
    "+39 06 698577777",
    "+7800-flowers",
    "tel:+78005553535"
  )

  val invalid = List(
    "88005553535",
    "+78005553535aaa",
    "something",
    "p78005553535",
    "+71005553535"
  )

  "Phone" should "parse valid international phone numbers" in {
    for (phone <- valid)
      assert(Phone.fromString(phone).isRight)
  }

  it should "not parse invalid or national phone numbers" in {
    for (phone <- invalid)
      assert(Phone.fromString(phone).isLeft)
  }
}