import {CREATE, DELETE, GET_LIST, GET_ONE, UPDATE} from 'react-admin';
import getOneEntry from './getOneEntry';
import getListOfEntries from './getListOfEntries';
import createEntry from './createEntry';
import updateEntry from './updateEntry';
import deleteEntry from './deleteEntry';

function dataProvider(type, resource, params) {
    switch (type) {
        case GET_ONE:
            return getOneEntry(params);
        case GET_LIST:
            return getListOfEntries(params);
        case CREATE:
            return createEntry(params);
        case UPDATE:
            return updateEntry(params);
        case DELETE:
            return deleteEntry(params);
        default:
            throw new Error(`Unsupported data provider request type ${type}`);
    }
}

export default dataProvider;