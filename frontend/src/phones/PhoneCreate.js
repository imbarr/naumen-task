import React from 'react';
import {Create, SimpleForm, TextInput} from 'react-admin';
import validatePhoneNumberString from './validatePhoneNumberString';

function required(value) {
    if (!value)
        return 'Field is required';
}

const PhoneCreate = (props) =>
    <Create {...props}>
        <SimpleForm redirect='/'>
            <TextInput source='name' validate={required}/>
            <TextInput source='phone' validate={(phone) => validatePhoneNumberString(phone) || required(phone)}/>
        </SimpleForm>
    </Create>;

export default PhoneCreate;