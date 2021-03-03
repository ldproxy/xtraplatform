const transform = (field, values, transformers, fromTo) =>
    transformers && transformers[field] && transformers[field][fromTo]
        ? transformers[field][fromTo](values[field])
        : values[field];

export const transformFrom = (field, values, transformers) =>
    transform(field, values, transformers, 'from');

export const transformTo = (field, values, transformers) =>
    transform(field, values, transformers, 'to');

const transformSplit = (field, values, transformers) => {
    if (transformers && transformers[field] && transformers[field].split) {
        const splitted = {
            ...values,
        };
        delete splitted[field];
        Object.keys(transformers[field].split).forEach((newField) => {
            const splittedValue = transformers[field].split[newField](values[field]);
            if (splittedValue !== undefined) splitted[newField] = splittedValue;
        });
        return splitted;
    }

    return values;
};

const isEmpty = (field) =>
    field === undefined ||
    field === null ||
    (typeof field === 'object' && Object.keys(field).length === 0);

const isSplitted = (field, fieldsTransformation) =>
    fieldsTransformation && fieldsTransformation[field] && fieldsTransformation[field].split;

const transformedFields = (fields, fieldsTransformation) => {
    let transformed = {
        ...fields,
    };

    Object.keys(fields).forEach((field) => {
        if (isSplitted(field, fieldsTransformation)) {
            transformed = transformSplit(field, transformed, fieldsTransformation);
        } else {
            transformed[field] = transformFrom(field, transformed, fieldsTransformation);
        }
    });

    return transformed;
};

const reverseTransformedFields = (fields, defaulted, changed, fieldsTransformation) => {
    const transformed = {
        ...changed,
    };
    //console.log('TRANS1', transformed);

    Object.keys(transformed).forEach((field) => {
        transformed[field] = transformTo(field, transformed, fieldsTransformation);
    });
    //console.log('TRANS2', transformed);

    Object.keys(fieldsTransformation).forEach((field) => {
        if (
            !transformed.hasOwnProperty(field) &&
            fieldsTransformation[field].split &&
            fieldsTransformation[field].merge
        ) {
            if (
                Object.keys(fieldsTransformation[field].split).filter((key) =>
                    transformed.hasOwnProperty(key)
                ).length > 0
            ) {
                const splitted = Object.keys(fieldsTransformation[field].split).map((key) =>
                    transformed.hasOwnProperty(key) ? transformed[key] : fields[key]
                );

                if (
                    Object.keys(fieldsTransformation[field].split).filter((key) => !defaulted[key])
                        .length > 0
                ) {
                    transformed[field] = fieldsTransformation[field].merge(...splitted);
                } else {
                    transformed[field] = null;
                }

                Object.keys(fieldsTransformation[field].split).forEach(
                    (key) => delete transformed[key]
                );
            }
        }
    });
    //console.log('TRANS3', transformed);

    // defaults are subtracted by backend, so sending null is no longer needed
    /*Object.keys(transformed).forEach((field) => {
        if (defaulted[field]) {
            transformed[field] = null;
        }
    });*/
    //console.log('TRANS4', transformed);

    return transformed;
};

export const mergedFields = (fields, fieldsDefault, fieldsTransformation) => {
    const merged = {
        ...fields,
    };

    Object.keys(fields).forEach((field) => {
        if (isEmpty(fields[field]) && fieldsDefault.hasOwnProperty(field)) {
            merged[field] = fieldsDefault[field];
        }
    });

    return transformedFields(merged, fieldsTransformation);
};

const nullIfEmpty = (value) => (value === '' ? null : value);

export const changedFields = (fields, touched, defaulted, fieldsTransformation) => {
    let changed = null;

    Object.keys(touched).forEach((field) => {
        if (!changed) changed = {};

        changed[field] = nullIfEmpty(fields[field]);
    });

    return reverseTransformedFields(fields, defaulted, changed, fieldsTransformation);
};

export const defaultedFields = (fields, fieldsDefault, fieldsTransformation) => {
    const defaulted = {};

    const transformedDefaults = transformedFields(fieldsDefault, fieldsTransformation);

    Object.keys(fields).forEach((field) => {
        if (transformedDefaults.hasOwnProperty(field)) {
            const value = transformTo(field, fields, fieldsTransformation);
            if (isEmpty(value) || value === transformedDefaults[field]) {
                defaulted[field] = true;
            }
        }
    });

    return defaulted;
};

export const getFieldsDefault = (fields, defaults) => {
    const fieldsDefault = {};

    if (!defaults) return fieldsDefault;

    Object.keys(fields).forEach((field) => {
        if (defaults.hasOwnProperty(field) && !isEmpty(defaults[field])) {
            fieldsDefault[field] = defaults[field];
        }
    });

    return fieldsDefault;
};
