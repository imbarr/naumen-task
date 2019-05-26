import config from '../config';
import axios from 'axios'

function getOneEntry(params) {
    let request = {
        method: 'GET',
        url: `${config.serverURL}/phonebook?${params.id}`
    };
    return axios(request).then(
        response => ({
            data: {
                id: params.id,
                name: response.data.name,
                phone: response.data.phone
            }
        })
    )
}

export default getOneEntry;