import {PhoneNumberUtil} from 'google-libphonenumber';

const parser = PhoneNumberUtil.getInstance();
const unknownRegion = 'ZZ';

function validatePhoneNumber(phoneNumber) {
    if (!parser.isValidNumber(phoneNumber)) {
        return 'Phone number is parsed, but invalid';
    }
    if (phoneNumber.hasExtension()) {
        return 'Phone number extentions are not allowed';
    }
}

function validatePhoneNumberString(string) {
    if (string) {
        try {
            let phoneNumber = parser.parse(string, unknownRegion);
            return validatePhoneNumber(phoneNumber)
        } catch (_) {
            return 'Phone number cannot be parsed'
        }
    }
}

export default validatePhoneNumberString;