import { normalize, schema } from 'normalizr';

var commonProps = ['id']; // code

var ftProps = commonProps.concat(['name', 'namespace', 'displayName', 'mappings']);

var serviceProps = commonProps.concat(['type', 'name', 'description', 'status', 'featureTypes', 'serviceProperties', 'dateCreated', 'wfsAdapter']);

function filter(include, exclude, entity, parent) {
    var idFound = false
    var mapFound = false
    var wfsFound = false
    for (var key in entity) {
        if (include.indexOf(key) === -1) {
            delete entity[key];
            continue;
        }
        if ((!include || include.length === 0) && exclude.indexOf(key) !== -1) {
            delete entity[key];
            continue;
        }

        if (key === 'id')
            idFound = true
        if (key === 'mappings')
            mapFound = true
        if (key === 'wfsAdapter')
            wfsFound = true
    }
    if (!idFound && entity.namespace && entity.name) {
        entity.id = parent.id + '_' + entity.namespace + ':' + entity.name
        entity.qn = entity.namespace + ':' + entity.name
    }
    if (mapFound) {
        entity.mappings = entity.mappings.mappings
        for (var key in entity.mappings) {
            entity.mappings[key].id = entity.id;
            if (key !== entity.qn)
                entity.mappings[key].id += '_' + key;
            entity.mappings[key].qn = key;
        }
    }
    if (wfsFound) {
        entity.nameSpaces = {}
        for (var key in entity.wfsAdapter.nsStore.namespaces) {
            var newKey = entity.wfsAdapter.nsStore.namespaces[key];
            entity.nameSpaces[newKey] = key;
        }
        delete entity['wfsAdapter'];
    }
    return entity;
}


const mappingSchema = new schema.Entity('mappings', {}, {
    //idAttribute: (value, parent, key) => parent.id + '_' + value.id
    //processStrategy: filter.bind(null, [], [])
});

const ftSchema = new schema.Entity('featureTypes', {
    mappings: new schema.Array(mappingSchema)
}, {
    //idAttribute: (value, parent, key) => parent.id + '_' + value.id,
    processStrategy: filter.bind(null, ftProps, [])
});

const serviceConfigSchema = new schema.Entity('serviceConfigs', {
    featureTypes: new schema.Array(ftSchema)
}, {
    processStrategy: filter.bind(null, serviceProps, [])
});

const serviceConfigListSchema = new schema.Array(serviceConfigSchema);

const serviceSchema = new schema.Entity('services', {
    featureTypes: new schema.Array(ftSchema)
}, {
    processStrategy: filter.bind(null, serviceProps, [])
});

const serviceListSchema = new schema.Array(serviceSchema);

export default function normalize2(services) {
    return normalize(services, serviceListSchema);
}

export const normalizeServices = function(services) {
    return normalize(services, serviceListSchema);
}

export const normalizeServiceConfigs = function(services) {
    return normalize(services, serviceConfigListSchema);
}

