import {CREATE, DELETE, GET_LIST, GET_ONE, UPDATE} from 'react-admin';
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
                end: page * perPage - 1,
                nameSubstring: params.filter.nameSubstring,
                phoneSubstring: params.filter.phoneSubstring
            };
            let queryString = stringify(query);
            return {
                method: 'GET',
                url: `${config.serverURL}/phonebook?${queryString}`
            };
        case GET_ONE:
            return {
                method: 'GET',
                url: `${config.serverURL}/phonebook?${params.id}`
            };
        case CREATE:
            return {
                method: 'POST',
                url: `${config.serverURL}/phonebook`,
                data: JSON.stringify(params.data),
                headers: {
                    'Content-Type': 'application/json',
                }
            };
        case UPDATE:
            return {
                method: 'PATCH',
                url: `${config.serverURL}/phonebook/${params.id}`,
                data: JSON.stringify(params.data),
                headers: {
                    'Content-Type': 'application/json',
                }
            };
        case DELETE:
            return {
                method: 'DELETE',
                url: `${config.serverURL}/phonebook/${params.id}`
            };
        default:
            throw new Error('Unsupported data provider request type' + type)
    }
}

function fromResponse(response, type, resource, params) {
    switch (type) {
        case GET_LIST:
            let total = response.headers['x-total-count'];
            return {
                data: response.data,
                total: parseInt(total, 10)
            };
        case GET_ONE:
            return {
                data: {
                    id: params.id,
                    name: response.data.name,
                    phone: response.data.phone
                }
            };
        case CREATE:
            let id = response.headers['location'].split('/').pop();
            return {
                data: {
                    id: parseInt(id, 10),
                    name: params.data.name,
                    phone: params.data.phoneNumber
                }
            };
        case UPDATE:
            return {
                data: {
                    id: params.id,
                    name: params.data.name,
                    phone: params.data.phoneNumber
                }
            };
        case DELETE:
            return {
                data: {
                    id: params.id,
                    name: params.previousData.name,
                    phone: params.previousData.phone
                }
            };
        default:
            throw new Error('Unsupported data provider request type: ' + type)
    }
}

export default dataProvider