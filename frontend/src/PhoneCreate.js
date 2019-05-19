import React from 'react';
import {Create, SimpleForm, TextInput} from 'react-admin';

const PhoneCreate = props =>
    <Create {...props}>
        <SimpleForm redirect='/'>
            <TextInput source='name' validate={required}/>
            <TextInput source='phoneNumber' validate={required}/>
        </SimpleForm>
    </Create>;

function required(value) {
    return value ? undefined : 'Field is required'
}

export default PhoneCreate