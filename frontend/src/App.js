import React from 'react';
import {Admin, Resource} from 'react-admin';
import dataProvider from './DataProvider';
import PhoneList from './PhoneList';

const App = () =>
    <Admin dataProvider={dataProvider}>
        <Resource name="Phone Numbers" list={PhoneList}/>
    </Admin>;

export default App