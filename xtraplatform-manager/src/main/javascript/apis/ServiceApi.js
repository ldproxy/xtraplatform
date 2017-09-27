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
    }
}

export default ServiceApi;