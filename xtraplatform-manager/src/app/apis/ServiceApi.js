import 'whatwg-fetch'

//import services from '../assets/services'
import serviceIds from '../assets/service-ids'
import serviceAdmin from '../assets/service-admin'
import serviceConfig from '../assets/service-config'

class ServiceApi {

    static getServices() {
        return fetch(`/rest/admin/services/`)
            .then(handleErrors)
            .then(parseJSON)
    /*.catch(function(error) {
        console.log('request failed', error)
    })*/
    /*return new Promise((resolve) => {
        setTimeout(() => {
            resolve(serviceIds);
        }, 0);
    });*/
    }
    static getService(id) {
        return fetch(`/rest/admin/services/${id}/`)
            .then(handleErrors)
            .then(parseJSON)
    /*return new Promise((resolve) => {
        setTimeout(() => {
            resolve(serviceAdmin);
        }, 0);
    });*/
    }
    static getServiceConfig(id) {
        return fetch(`/rest/admin/services/${id}/config/`)
            .then(handleErrors)
            .then(parseJSON)
    /*return new Promise((resolve) => {
        setTimeout(() => {
            //resolve(services[id]);
            resolve(serviceConfig);
        }, 1000);
    });*/
    }

    static postServiceConfig(config) {
        let {id, ...body} = config;

        return fetch(`/rest/admin/services/${id}/`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                ...body,
                targetStatus: body.targetStatus ? body.targetStatus : null
            })
        })
            //TODO
            //.then(handleErrors)
            .then(ServiceApi.getServiceConfig.bind(null, id))
    }

    static deleteService(id) {

        return fetch(`/rest/admin/services/${id}/`, {
            method: 'DELETE'
        })
            .then(handleErrors)
    }

    static postService(params) {
        return fetch(`/rest/admin/services/`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        })
            .then(handleErrors)
    }
}

function handleErrors(response) {
    if (!response.ok) {
        return parseJSON(response)
            .then(json => {
                var error = new Error(response.statusText)
                error.response = json && json.error || {}
                throw error
            })
    }
    return response;
}

function parseJSON(response) {
    return response.json()
}



export default ServiceApi;