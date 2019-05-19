import React from 'react';
import {Admin, Resource} from 'react-admin';
import dataProvider from './DataProvider';
import PhoneList from './PhoneList';
import PhoneCreate from "./PhoneCreate";
import PhoneEdit from "./PhoneEdit";

const App = () =>
    <Admin dataProvider={dataProvider}>
        <Resource name="Phone Numbers" create={PhoneCreate} edit={PhoneEdit} list={PhoneList}/>
    </Admin>;

export default App