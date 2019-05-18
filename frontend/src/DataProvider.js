import {GET_LIST} from 'react-admin';
import config from "./config"
import axios from "axios"
import {stringify} from 'query-string';

function dataProvider(type, resource, params) {
    let request = getRequest(type, resource, params);
    return axios(request).then(
        response =>
            fromResponse(response, type, resource, params),
        reason => {
            throw reason
        })
}

function getRequest(type, resource, params) {
    switch (type) {
        case GET_LIST:
            let {page, perPage} = params.pagination;
            let query = {
                start: (page - 1) * perPage,
                end: page * perPage - 1
            };
            let queryString = stringify(query);
            return {
                method: 'GET',
                url: `${config.serverURL}/phonebook?${queryString}`
            };
        default:
            throw new Error('Unsupported data provider request type')
    }
}

function fromResponse(response, type, resource, params) {
    switch (type) {
        case GET_LIST:
            return {data: response.data, total: 100};
        default:
            throw new Error('Unsupported data provider request type')
    }
}

export default dataProvider