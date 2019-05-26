import React from 'react';
import {Admin, Resource} from 'react-admin';
import dataProvider from './dataProvider';
import PhoneList from './phones/PhoneList';
import PhoneCreate from './phones/PhoneCreate';
import PhoneEdit from './phones/PhoneEdit';

const App = () =>
    <Admin dataProvider={dataProvider}>
        <Resource name="Phone Numbers" create={PhoneCreate} edit={PhoneEdit} list={PhoneList}/>
    </Admin>;

export default App;