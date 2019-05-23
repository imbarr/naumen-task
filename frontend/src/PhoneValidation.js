import {PhoneNumberUtil} from 'google-libphonenumber'

const parser = PhoneNumberUtil.getInstance();
const unknownRegion = 'ZZ';

export function isValid(phone) {
    if (phone) {
        try {
            let number = parser.parse(phone, unknownRegion);
            if (!parser.isValidNumber(number))
                return 'Phone number is parsed, but invalid';
            if (number.hasExtension())
                return 'Phone number extentions are not allowed';
        } catch (_) {
            return 'Phone number cannot be parsed'
        }
    }
}