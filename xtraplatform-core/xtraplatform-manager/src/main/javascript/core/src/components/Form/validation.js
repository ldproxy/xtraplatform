const exists = (value) => value !== null && value !== undefined;

export const required = () => (value) => (!exists(value) || value.length === 0 ? 'required' : null);

export const equals = (otherKey, otherLabel) => (value, otherValues) =>
    exists(value) && value.length > 0 && value !== otherValues[otherKey]
        ? `does not equal ${otherLabel || otherKey}`
        : null;

export const differs = (otherKey, otherLabel) => (value, otherValues) =>
    exists(value) && value.length > 0 && value === otherValues[otherKey]
        ? `may not equal ${otherLabel || otherKey}`
        : null;

export const minLength = (length) => (value) =>
    !exists(value) || value.length < length ? `at least ${length} characters are required` : null;

export const maxLength = (length) => (value) =>
    exists(value) && value.length > length ? `no more than ${length} characters are allowed` : null;

export const allowedChars = (pattern) => (value) =>
    exists(value) && value.match(new RegExp(`[^${pattern}]`))
        ? `character '${value.match(new RegExp(`[^${pattern}]`))}' is not allowed`
        : null;

export const forbiddenChars = (pattern) => (value) =>
    exists(value) && value.match(new RegExp(`[${pattern}]`))
        ? `character '${value.match(new RegExp(`[${pattern}]`))}' is not allowed`
        : null;

export const url = () => (value) =>
    exists(value) &&
    value.length > 0 &&
    !value.match(/^https?:\/\/[\w.-]+(?:\.[\w\.-]+)?[\w\-\._~:/?#[\]@!\$&'\(\)\*\+,;=.]+$/i)
        ? `invalid URL`
        : null;

export const ifEqualsThen = (otherKey, otherValue, nested) => (value, otherValues) =>
    otherValues[otherKey] === otherValue ? nested(value, otherValues) : null;

export const isFloat = () => (value) =>
    exists(value) && !(typeof value === 'number' || value.match(/^[0-9]+(\.[0-9]+)?$/i))
        ? `invalid number`
        : null;

export const bounds = (min, max, msg) =>
    existsAnd((value) => value < min || value > max, 'out of bounds', msg);

export const lessThan = (otherKey, msg) =>
    existsAnd((value, values) => value >= values[otherKey], `not less than '${msg || otherKey}'`);

export const greaterThan = (otherKey, msg) =>
    existsAnd(
        (value, values) => value <= values[otherKey],
        `not greater than '${msg || otherKey}'`
    );

export const isInt = (msg) =>
    existsAnd(
        (value) => !(typeof value === 'number' || value.match(/^[0-9]+$/i)),
        'invalid number',
        msg
    );

const existsAnd = (test, defaultMessage, customMessage) => (value, values) =>
    exists(value) && test(value, values) ? customMessage || defaultMessage : null;

const notExistsOr = (test, defaultMessage, customMessage) => (value, values) =>
    !exists(value) || test(value, values) ? customMessage || defaultMessage : null;

export const objectMap = (o, f) =>
    Object.assign({}, ...Object.keys(o).map((k) => ({ [k]: f(k, o[k]) })));

const reduced2 = (validators) =>
    objectMap(validators, (key, vldtr) => {
        if (Array.isArray(vldtr)) {
            return (value, otherValues, props) =>
                vldtr.reduce(
                    (prev, func) => (prev !== null ? prev : func(value, otherValues, props)),
                    null
                );
        }
        return vldtr;
    });

const validateErrors = (values, validations, all = false) =>
    all
        ? objectMap(validations, (key, validateField) => validateField(values[key], values))
        : objectMap(values, (key, value) =>
              validations[key] ? validations[key](value, values) : null
          );

export const validate = (values, validations) => {
    const finalValidations = reduced2(validations);
    const currentErrors = validateErrors(values, finalValidations, false);
    const allErrors = validateErrors(values, finalValidations, true);

    return {
        valid: Object.keys(allErrors).filter((key) => !!allErrors[key]).length === 0,
        errors: currentErrors,
    };
};
