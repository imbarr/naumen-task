import React from 'react';
import {Edit, SimpleForm, TextInput, DisabledInput} from 'react-admin';

function atLeastOne(value) {
    if (!value.name && !value.phoneNumber) {
        return {name: 'At least one field should be present'}
    }
}

const PhoneEdit = props =>
    <Edit {...props}>
        <SimpleForm validate={atLeastOne}>
            <DisabledInput source="id"/>
            <TextInput source='name'/>
            <TextInput source='phoneNumber'/>
        </SimpleForm>
    </Edit>;

export default PhoneEdit