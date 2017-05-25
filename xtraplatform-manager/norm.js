import { normalize, schema, arrayOf } from 'normalizr';
import services from './src/app/assets/services2.json'

var commonProps = ['id']; // code

var ftProps = commonProps.concat(['name', 'namespace', 'displayName', 'mappings']); // code

var serviceProps = commonProps.concat(['type', 'name', 'description', 'status', 'featureTypes']); // code, requests

var testStatProps = serviceProps.concat(['duration', 'pass', 'fail', 'pending', 'skipped', 'err', 'stats', 'reportTitle']); // code, requests

function filter(include, exclude, entity, parent) {
    var idFound = false
    var mapFound = false
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
    }
    if (!idFound && entity.namespace && entity.name) {
        entity.id = parent.id + '_' + entity.namespace + ':' + entity.name
        entity.qn = entity.namespace + ':' + entity.name
    }
    if (mapFound) {
        entity.mappings = entity.mappings.mappings
        for (var key in entity.mappings) {
            entity.mappings[key].id = key;
        }
    }
    return entity;
}

function generateId(entity, id) {
    return entity.reportTitle + '_' + entity.stats.end;
}



const mappingSchema = new schema.Entity('mappings', {}, {
    idAttribute: (value, parent, key) => parent.id + '_' + value.id
//processStrategy: filter.bind(null, [], [])
});

const ftSchema = new schema.Entity('featureTypes', {
    mappings: new schema.Array(mappingSchema)
}, {
    //idAttribute: (value, parent, key) => parent.id + '_' + value.id,
    processStrategy: filter.bind(null, ftProps, [])
});

const serviceSchema = new schema.Entity('services', {
    featureTypes: new schema.Array(ftSchema)
}, {
    processStrategy: filter.bind(null, serviceProps, [])
});

const serviceListSchema = new schema.Array(serviceSchema);


console.log(JSON.stringify(normalize(services.slice(0, 1), serviceListSchema), null, 2))


/*
TODO: 
 - design store models
 - action creators with redux-actions
 - refactor reducers
 - selectors with reselect
 */
