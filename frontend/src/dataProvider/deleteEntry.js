import config from '../config';
import axios from 'axios'

function deleteEntry(params) {
    let request = {
        method: 'DELETE',
        url: `${config.serverURL}/phonebook/${params.id}`
    };
    return axios(request).then(
        () => ({
            data: {
                id: params.id,
                name: params.previousData.name,
                phone: params.previousData.phone
            }
        })
    )
}

export default deleteEntry;