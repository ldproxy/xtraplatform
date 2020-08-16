import checkPropTypes from 'check-prop-types';

const validatePropTypes = (schema) => resource => {
    if (Array.isArray(resource)) {
        for (let res of resource) {
            const result = checkPropTypes(schema, res, 'resource prop', 'contract');
            if (result) return result
        }
    } else {
        const result = checkPropTypes(schema, resource, 'resource prop', 'contract');
        if (result) return result
    }

    return null
}

export {
    validatePropTypes
};
