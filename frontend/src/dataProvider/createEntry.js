import config from '../config';
import axios from 'axios';

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

function createEntry(params) {
    let request = {
        method: 'POST',
        url: `${config.serverURL}/phonebook`,
        data: JSON.stringify(params.data),
        headers: {
            'Content-Type': 'application/json',
        }
    };
    return axios(request).then((response) => fromCreateResponse(response, params));
}

export default createEntry;