import React from 'react';
import {Datagrid, List, NumberField, TextField, EditButton, DeleteButton} from 'react-admin';
import PhoneFilter from './PhoneFilter'

const PhoneList = props =>
    <List {...props} filters={<PhoneFilter/>} bulkActionButtons={false}>
        <Datagrid>
            <NumberField source="id" sortable={false} textAlign='left'/>
            <TextField source="name" sortable={false}/>
            <TextField source="phoneNumber" sortable={false}/>
            <EditButton/>
            <DeleteButton undoable={false}/>
        </Datagrid>
    </List>;

export default PhoneList