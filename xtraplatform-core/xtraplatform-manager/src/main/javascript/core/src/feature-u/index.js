import checkPropTypes from 'check-prop-types';

export const validatePropTypes = (schema) => (resource) => {
    if (Array.isArray(resource)) {
        for (let i = 0; i < resource.length; i++) {
            const result = checkPropTypes(schema, resource[i], 'resource prop', 'contract');
            if (result) return result;
        }
    } else {
        const result = checkPropTypes(schema, resource, 'resource prop', 'contract');
        if (result) return result;
    }

    return null;
};
