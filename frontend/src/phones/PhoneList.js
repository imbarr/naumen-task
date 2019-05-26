import React from 'react';
import {
    Datagrid,
    List,
    NumberField,
    TextField,
    EditButton,
    DeleteButton,
    Filter,
    TextInput,
    Pagination
} from 'react-admin';

const PhoneFilter = props =>
    <Filter {...props}>
        <TextInput label='Name Substring' source='nameSubstring' alwaysOn/>
        <TextInput label='Phone Substring' source='phoneSubstring' alwaysOn/>
    </Filter>;

const PhonePagination = props =>
    <Pagination rowsPerPageOptions={[10, 25, 50, 100]} {...props}/>;

const PhoneList = props =>
    <List {...props} filters={<PhoneFilter/>} pagination={<PhonePagination/>} bulkActionButtons={false}>
        <Datagrid>
            <NumberField source="id" sortable={false} textAlign='left'/>
            <TextField source="name" sortable={false}/>
            <TextField source="phone" sortable={false}/>
            <EditButton/>
            <DeleteButton undoable={false}/>
        </Datagrid>
    </List>;

export default PhoneList