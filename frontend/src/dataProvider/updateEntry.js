import config from '../config';
import axios from 'axios'

function updateEntry(params) {
    let request = {
        method: 'PATCH',
        url: `${config.serverURL}/phonebook/${params.id}`,
        data: JSON.stringify(params.data),
        headers: {
            'Content-Type': 'application/json',
        }
    };
    return axios(request).then(
        () => ({
            data: {
                id: params.id,
                name: params.data.name,
                phone: params.data.phone
            }
        })
    )
}

export default updateEntry;