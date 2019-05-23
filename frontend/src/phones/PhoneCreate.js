import React from 'react';
import {Create, SimpleForm, TextInput} from 'react-admin';
import {isValid} from "../PhoneValidation";

const PhoneCreate = props =>
    <Create {...props}>
        <SimpleForm redirect='/'>
            <TextInput source='name' validate={required}/>
            <TextInput source='phone' validate={phone => isValid(phone) || required(phone)}/>
        </SimpleForm>
    </Create>;

function required(value) {
    return value ? undefined : 'Field is required'
}

export default PhoneCreate