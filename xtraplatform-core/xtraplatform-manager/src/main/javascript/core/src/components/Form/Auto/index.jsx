import React, { useState, useRef, useCallback, useEffect } from 'react';
import PropTypes from 'prop-types';

import { Form } from 'grommet';
import { useDebounce, useOnChange } from '../../../hooks';
import { mergedFields, defaultedFields, changedFields } from './fields';
import { validate, objectMap } from '../validation';

const AutoForm = ({
    fields,
    fieldsDefault,
    fieldsTransformation,
    fieldsValidation,
    inheritedLabel,
    values: extValues,
    setValues: setExtValues,
    debounce,
    children,
    onPending,
    onChange,
    onSubmit: extOnSubmit,
    onCancel: extOnCancel,
}) => {
    const initialFields = mergedFields(fields, fieldsDefault, fieldsTransformation);
    const [intValues, setIntState] = useState(initialFields);
    const [touched, setTouched] = useState({});

    // use external state if given, internal otherwise
    const values = extValues && setExtValues ? extValues : intValues;
    const setValues = useCallback(
        (change) => {
            onPending();
            extValues && setExtValues ? setExtValues(change) : setIntState(change);
        },
        [onPending, extValues, setExtValues, setIntState]
    );

    const defaulted = defaultedFields(values, fieldsDefault, fieldsTransformation);
    const { valid, errors } = validate(values, fieldsValidation);

    // initialize validation
    const forceAllFieldValidations = useCallback(() => {
        if (!valid) {
            const hasMissingKeys =
                Object.keys(fieldsValidation).filter((key) => !values.hasOwnProperty(key)).length >
                0;
            if (hasMissingKeys) {
                const initMissingValues = objectMap(fieldsValidation, () => '');
                setValues({ ...initMissingValues, ...values });
            }
            return;
        }
    }, [fieldsValidation, values, setValues, valid]);

    const form = useRef(null);
    const reset = useCallback(() => form.current && form.current.reset(), []);

    const submit = useCallback(
        (changes) => {
            if (!valid || !changes || Object.keys(changes).length === 0) {
                return;
            }

            if (extOnSubmit) {
                extOnSubmit(changes, reset);
            } else if (onChange) {
                onChange(changes);
            }

            setTouched({});
        },
        [valid, setTouched, /*TODO onChange,*/ extOnSubmit, reset]
    );

    // calculate changes, submit with optional debounce if changes are found
    const changes = !valid ? {} : changedFields(values, touched, defaulted, fieldsTransformation);
    const useSubmit = debounce > 0 ? useDebounce : extOnSubmit ? () => {} : useOnChange;
    useSubmit(changes, submit, true, debounce);

    useEffect(() => {
        if (!extOnSubmit && changes && Object.keys(changes).length > 0) {
            forceAllFieldValidations();
        }
    }, [extOnSubmit, changes, forceAllFieldValidations]);

    //TODO: replace with context
    const newChildren = React.Children.map(children, (child) => {
        if (
            child &&
            (defaulted[child.props.name] ||
                values[child.props.name] === true ||
                errors[child.props.name])
        ) {
            return React.cloneElement(child, {
                inheritedFrom: defaulted[child.props.name] ? inheritedLabel : null,
                truthful: values[child.props.name] === true,
                error: errors[child.props.name],
            });
        }
        return child;
    });

    return (
        <Form
            ref={form}
            value={values}
            onChange={(values, { touched }) => {
                setValues(values);
                setTouched(touched);
            }}
            onSubmit={() => {
                forceAllFieldValidations();
                submit(changes);
            }}
            onReset={extOnCancel}>
            {newChildren}
        </Form>
    );
};

AutoForm.propTypes = {
    fields: PropTypes.object.isRequired,
    fieldsDefault: PropTypes.object,
    fieldsTransformation: PropTypes.object,
    fieldsValidation: PropTypes.object,
    inheritedLabel: PropTypes.string,
    values: PropTypes.object,
    setValues: PropTypes.func,
    debounce: PropTypes.number,
    children: PropTypes.oneOfType([PropTypes.element, PropTypes.arrayOf(PropTypes.element)])
        .isRequired,
    onPending: PropTypes.func,
    onChange: PropTypes.func.isRequired,
};

AutoForm.defaultProps = {
    fieldsDefault: {},
    fieldsTransformation: {},
    fieldsValidation: {},
    inheritedLabel: 'unknown',
    values: null,
    setValues: null,
    debounce: 0,
    onPending: () => {},
};

AutoForm.displayName = 'AutoForm';

export default AutoForm;
