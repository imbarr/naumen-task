import React from 'react';
import {Edit, SimpleForm, TextInput, DisabledInput} from 'react-admin';
import {isValid} from "../PhoneValidation";

function atLeastOne(value) {
    if (!value.name && !value.phone) {
        return {name: 'At least one field should be present'}
    }
}

const PhoneEdit = props =>
    <Edit {...props} undoable={false}>
        <SimpleForm validate={atLeastOne}>
            <DisabledInput source='id'/>
            <TextInput source='name'/>
            <TextInput source='phone' validate={isValid}/>
        </SimpleForm>
    </Edit>;

export default PhoneEdit