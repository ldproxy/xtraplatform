import React, { useState, useRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Box, Form, Button } from 'grommet';
import { useDebounce, useOnChange } from '../../../hooks';
import { mergedFields, defaultedFields, changedFields } from './fields';
import { validate, objectMap } from '../validation';

const HiddenButton = styled(Button)`
    display: none;
`;

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
}) => {
    const [intValues, setIntState] = useState(
        mergedFields(fields, fieldsDefault, fieldsTransformation)
    );

    const values = extValues && setExtValues ? extValues : intValues;
    const setValues = (change) => {
        onPending();
        extValues && setExtValues ? setExtValues(change) : setIntState(change);
    };
    const defaulted = defaultedFields(values, fieldsDefault, fieldsTransformation);
    const { valid, errors } = validate(values, fieldsValidation);
    //console.log('VALID', valid, errors);

    // workaround with hidden button because Safari does not support form.requestSubmit()
    const submitButton = useRef(null);
    const submit = useCallback(() => submitButton.current.click(), [submitButton]);

    const useSubmit = debounce > 0 ? useDebounce : useOnChange;

    useSubmit(values, submit, debounce);

    const onSubmit = ({ value, touched }) => {
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
        console.log('SAVE', value, touched, defaulted);
        const changes = changedFields(value, touched, defaulted, fieldsTransformation);

        if (onChange && changes) onChange(changes);
    };

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
        <Form value={values} onChange={setValues} onSubmit={onSubmit}>
            <HiddenButton type='submit' ref={submitButton} onClick={(e) => e.stopPropagation()} />
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
