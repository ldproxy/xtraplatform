import React from 'react';
import PropTypes from 'prop-types';

import { RadioButtonGroup } from 'grommet';
import FormField from '../Field';
import InfoLabel from '../../InfoLabel';

const RadioField = ({
    label,
    help,
    inheritedFrom,
    color,
    error,
    options,
    name,
    disabled,
    ...rest
}) => {
    return (
        <FormField
            label={label}
            help={help}
            inheritedFrom={inheritedFrom}
            color={color}
            error={error}>
            <RadioButtonGroup
                name={name}
                options={options}
                direction='row'
                focusIndicator={false}
                disabled={disabled}
                {...rest}
            />
        </FormField>
    );
};

RadioField.propTypes = {
    ...InfoLabel.propTypes,
    name: PropTypes.string.isRequired,
    truthful: PropTypes.bool,
};

RadioField.defaultProps = {
    ...InfoLabel.defaultProps,
    truthful: false,
};

RadioField.displayName = 'RadioField';

export default RadioField;
