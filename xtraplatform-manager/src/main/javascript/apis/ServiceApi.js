import 'whatwg-fetch'

//import services from '../assets/services'
import serviceIds from '../assets/service-ids'
import serviceAdmin from '../assets/service-admin'
import serviceConfig from '../assets/service-config'

import update from 'immutability-helper';
import { normalizeServices, normalizeServiceConfigs } from './ServiceNormalizer'

const ServiceApi = {

    getServicesQuery: function() {
        return {
            url: `/rest/admin/services/`,
            transform: (serviceIds) => ({
                serviceIds: serviceIds
            }),
            update: {
                serviceIds: (prev, next) => next
            }
        // TODO: force: true
        }
    },

    getServiceQuery: function(id) {
        return {
            url: `/rest/admin/services/${id}/`,
            transform: (service) => normalizeServices([service]).entities,
            update: {
                services: (prev, next) => {
                    return {
                        ...prev,
                        ...next
                    }
                }
            },
        //force: true
        }
    },

    getServiceConfigQuery: function(id) {
        return {
            url: `/rest/admin/services/${id}/config/`,
            transform: (serviceConfig) => normalizeServiceConfigs([serviceConfig]).entities,
            update: {
                serviceConfigs: (prev, next) => next,
                featureTypes: (prev, next) => next,
                mappings: (prev, next) => next
            },
            force: true
        }
    },

    addServiceQuery: function(service) {
        return {
            url: `/rest/admin/services/`,
            body: JSON.stringify(service),
            options: {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            },
            optimisticUpdate: {
                services: (prev) => Object.assign({}, prev, {
                    [service.id]: {
                        ...service,
                        name: service.id,
                        status: 'INITIALIZING',
                        dateCreated: Date.now()
                    }
                })
            },
            rollback: {
                services: (initialValue, currentValue) => {
                    const {[service.id]: deletedItem, ...rest} = currentValue
                    return rest;
                }
            }
        }
    },

    updateServiceQuery: function(service) {
        return {
            url: `/rest/admin/services/${service.id}/`,
            body: JSON.stringify(service),
            options: {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            },
            optimisticUpdate: {
                services: (prev) => Object.assign({}, prev, {
                    [service.id]: {
                        ...prev[service.id],
                        ...service,
                        dateModified: Date.now()
                    }
                }),
                serviceConfigs: (prev) => Object.assign({}, prev, {
                    [service.id]: {
                        ...prev[service.id],
                        ...service,
                        dateModified: Date.now()
                    }
                })
            }
        }
    },

    deleteServiceQuery: function(service) {
        return {
            url: `/rest/admin/services/${service.id}/`,
            options: {
                method: 'DELETE'
            },
            optimisticUpdate: {
                services: (prev) => {
                    const next = Object.assign({}, prev);
                    delete next[service.id];
                    return next
                },
                serviceIds: (prev) => prev.filter(id => id !== service.id)
            }
        }
    },

    getServices: function() {
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
    },
    getService: function(id) {
        return fetch(`/rest/admin/services/${id}/`)
            .then(handleErrors)
            .then(parseJSON)
    /*return new Promise((resolve) => {
        setTimeout(() => {
            resolve(serviceAdmin);
        }, 0);
    });*/
    },
    getServiceConfig: function(id) {
        return fetch(`/rest/admin/services/${id}/config/`)
            .then(handleErrors)
            .then(parseJSON)
    /*return new Promise((resolve) => {
        setTimeout(() => {
            //resolve(services[id]);
            resolve(serviceConfig);
        }, 1000);
    });*/
    },

    postServiceConfig: function(config) {
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
    },

    deleteService: function(id) {

        return fetch(`/rest/admin/services/${id}/`, {
            method: 'DELETE'
        })
            .then(handleErrors)
    },

    postService: function(params) {
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