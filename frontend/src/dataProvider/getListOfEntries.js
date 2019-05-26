import config from '../config';
import {stringify} from 'query-string';
import axios from 'axios'

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

function getListOfEntries(params) {
    let request = getRequestForGetList(params);
    return axios(request).then(fromGetListResponse)
}

export default getListOfEntries;