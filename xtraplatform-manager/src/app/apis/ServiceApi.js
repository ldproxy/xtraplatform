import 'whatwg-fetch'

//import services from '../assets/services'
import serviceIds from '../assets/service-ids'
import serviceAdmin from '../assets/service-admin'
import serviceConfig from '../assets/service-config'

class ServiceApi {

    static getServices() {
        return fetch(`/rest/admin/services/`)
            .then(checkStatus)
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
            .then(checkStatus)
            .then(parseJSON)
    /*return new Promise((resolve) => {
        setTimeout(() => {
            resolve(serviceAdmin);
        }, 0);
    });*/
    }
    static getServiceConfig(id) {
        return fetch(`/rest/admin/services/${id}/config/`)
            .then(checkStatus)
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
            //.then(checkStatus)
            .then(ServiceApi.getServiceConfig.bind(null, id))
    }

    static deleteService(id) {

        return fetch(`/rest/admin/services/${id}/`, {
            method: 'DELETE'
        })
            .then(checkStatus)
    }

    static postService(params) {
        return fetch(`/rest/admin/services/`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(params)
        })
            .then(checkStatus)
    }
}

function checkStatus(response) {
    if (response.status >= 200 && response.status < 300) {
        return response
    } else {
        var error = new Error(response.statusText)
        error.response = response
        throw error
    }
}

function parseJSON(response) {
    return response.json()
}



export default ServiceApi;