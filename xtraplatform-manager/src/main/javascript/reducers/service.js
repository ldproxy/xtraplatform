import { createAction, handleActions } from 'redux-actions';
//import { normalize, Schema, arrayOf } from 'normalizr';
//import { createSelector } from 'reselect'
//import { actions as reportActions } from './reporter'

// action creators
export const actions = {
    fetchedServices: createAction('FETCH_SERVICES_SUCCESS'),
    fetchedService: createAction('FETCH_SERVICE_SUCCESS'),
    fetchFailed: createAction('FETCH_FAILED'),
    addFailed: createAction('ADD_FAILED'),
    clearMessage: createAction('message/clear'),
    selectService: createAction('service/select'),
    updateService: createAction('service/update'),
    addService: createAction('service/add'),
    removeService: createAction('service/remove'),
    selectProperty: createAction('property/select'),
    selectFeatureType: createAction('featureType/select')
};


// state
const initialState = {
    entities: {},
    result: [],
    selectedService: null,
    selectedFeatureType: null,
    selectedProperty: null,
    changes: {},
    messages: {}
}


// reducer
export default handleActions({
    [actions.fetchedServices]: fetchedServices,
    [actions.fetchedService]: fetchedService,
    [actions.fetchFailed]: fetchFailed,
    [actions.addFailed]: addFailed,
    [actions.clearMessage]: clearMessage,
    [actions.selectService]: selectService,
    [actions.updateService]: updateService,
    [actions.addService]: addService,
    [actions.removeService]: removeService,
    [actions.selectProperty]: selectProperty,
    [actions.selectFeatureType]: selectFeatureType
}, initialState);


function fetchedServices(state, action) {
    return Object.assign({}, state, action.payload)
}

function fetchedService(state, action) {
    return {
        ...state,
        entities: {
            ...state.entities,
            ...action.payload.entities,
            services: {
                ...state.entities.services,
                ...action.payload.entities.services
            },
            featureTypes: {
                ...state.entities.featureTypes,
                ...action.payload.entities.featureTypes
            },
            mappings: {
                ...state.entities.mappings,
                ...action.payload.entities.mappings
            }
        }
    }
/*return Object.assign({}, state, {
    entities: action.payload.entities
})*/
}

function fetchFailed(state, action) {
    return state
}

function addFailed(state, action) {
    let {[action.payload.id]: deletedItem, ...rest} = state.entities.services
    return {
        ...state,
        entities: {
            ...state.entities,
            services: rest
        },
        messages: {
            ...state.messages,
            [action.payload.id]: {
                ...action.payload
            }
        }
    }
}

function clearMessage(state, action) {
    let {[action.payload]: deletedItem, ...rest} = state.messages
    return {
        ...state,
        messages: rest
    }
}


function selectService(state, action) {
    return {
        ...state,
        selectedService: action.payload
    }
}

function selectFeatureType(state, action) {
    return {
        ...state,
        selectedFeatureType: action.payload,
        selectedProperty: action.payload !== state.selectedFeatureType ? action.payload : state.selectedProperty
    }
}

function selectProperty(state, action) {
    return {
        ...state,
        selectedProperty: action.payload
    }
}

function updateService(state, action) {
    return state; /*{
        ...state,
        changes: action.payload
    }*/
}

function addService(state, action) {
    return {
        ...state,
        entities: {
            ...state.entities,
            services: {
                ...state.entities.services,
                [action.payload.id]: {
                    ...action.payload,
                    name: action.payload.id,
                    status: 'INITIALIZING',
                    dateCreated: Date.now()
                }
            }
        }
    }
}

function removeService(state, action) {
    let {[action.payload.id]: deletedItem, ...rest} = state.entities.services
    return {
        ...state,
        entities: {
            ...state.entities,
            services: rest
        }
    }
}

//selectors
export const getSelectedService = (state) => state.service.selectedService
export const getSelectedFeatureType = (state) => state.service.selectedFeatureType
export const getSelectedProperty = (state) => state.service.selectedProperty
export const getServices = (state) => state.service.entities.services
export const getService = (state, id) => (id && state.service.entities.services) ? state.service.entities.services[id] : null //state.service.entities.services[state.service.selectedService]
export const getFeatureTypes = (state, id) => {
    const service = getService(state, id);
    let fts = [];
    if (service && service.featureTypes) {
        for (var i = 0; i < service.featureTypes.length; i++) {
            const key = service.featureTypes[i];
            //fts[key] = state.service.entities.featureTypes[key]
            fts.push(state.service.entities.featureTypes[key])
        }
        fts = fts.sort((a, b) => a.name > b.name ? 1 : -1);
    }
    return fts;
}
export const getFeatureType = (state, id, ftid) => {
    const service = getService(state, id);
    if (service && state.service.entities.featureTypes) {
        for (var key in state.service.entities.featureTypes) {
            if (state.service.entities.featureTypes[key].name === ftid) {
                return state.service.entities.featureTypes[key];
            }
        }
    }
    return null;
}

export const getMappingsForFeatureType = (state, id, ftid) => {
    const featureType = getFeatureType(state, id, ftid);
    let mappings = {}
    if (featureType && state.service.entities.mappings) {
        for (var i = 0; i < featureType.mappings.length; i++) {
            let mapping = state.service.entities.mappings[featureType.mappings[i]];
            let {id, index, ...rest} = mapping;
            mappings[mapping.id] = rest
        }
    }
    return mappings;
}

