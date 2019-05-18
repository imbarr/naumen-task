import React from 'react';
import {Datagrid, List, NumberField, TextField} from 'react-admin';

const PhoneList = props =>
    <List {...props} bulkActionButtons={false}>
        <Datagrid>
            <NumberField source="id" sortable={false}/>
            <TextField source="name" sortable={false}/>
            <TextField source="phoneNumber" sortable={false}/>
        </Datagrid>
    </List>;

export default PhoneList