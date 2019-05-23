import {CREATE, DELETE, GET_LIST, GET_ONE, UPDATE} from 'react-admin';
import config from "./config"
import axios from "axios"
import {stringify} from 'query-string';

function dataProvider(type, resource, params) {
    switch (type) {
        case GET_ONE:
            return getOne(params);
        case GET_LIST:
            return getList(params);
        case CREATE:
            return create(params);
        case UPDATE:
            return update(params);
        case DELETE:
            return deleteEntry(params);
        default:
            throw new Error(`Unsupported data provider request type ${type}`)
    }
}

function getOne(params) {
    return dataProviderFor(
        () => ({
            method: 'GET',
            url: `${config.serverURL}/phonebook?${params.id}`
        }),
        response => ({
            data: {
                id: params.id,
                name: response.data.name,
                phone: response.data.phone
            }
        })
    )
}

function getList(params) {
    return dataProviderFor(
        () => getRequestForGetList(params),
        fromGetListResponse
    )
}

function create(params) {
    return dataProviderFor(
        () => ({
            method: 'POST',
            url: `${config.serverURL}/phonebook`,
            data: JSON.stringify(params.data),
            headers: {
                'Content-Type': 'application/json',
            }
        }),
        response => fromCreateResponse(response, params)
    )
}

function update(params) {
    return dataProviderFor(
        () => ({
            method: 'PATCH',
            url: `${config.serverURL}/phonebook/${params.id}`,
            data: JSON.stringify(params.data),
            headers: {
                'Content-Type': 'application/json',
            }
        }),
        () => ({
            data: {
                id: params.id,
                name: params.data.name,
                phone: params.data.phone
            }
        })
    )
}

function deleteEntry(params) {
    return dataProviderFor(
        () => ({
            method: 'DELETE',
            url: `${config.serverURL}/phonebook/${params.id}`
        }),
        () => ({
            data: {
                id: params.id,
                name: params.previousData.name,
                phone: params.previousData.phone
            }
        })
    )
}

function dataProviderFor(getRequest, fromResponse) {
    let request = getRequest();
    return axios(request).then(
        response =>
            fromResponse(response),
        reason => {
            throw reason
        })
}

function getRequestForGetList(params) {
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
}

function fromGetListResponse(response) {
    let total = response.headers['x-total-count'];
    return {
        data: response.data,
        total: parseInt(total, 10)
    };
}

function fromCreateResponse(response, params) {
    let id = response.headers['location'].split('/').pop();
    return {
        data: {
            id: parseInt(id, 10),
            name: params.data.name,
            phone: params.data.phone
        }
    };
}

export default dataProvider