import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { TextInput, TextArea } from 'grommet';
import InfoLabel from '../../InfoLabel';
import Field from '../Field';
import { useAutofocus } from '../../../hooks';

const StyledTextInput = styled(TextInput)`
    border: 0;
    ${(props) => props.inheritedFrom && `color: ${props.theme.global.colors['dark-6']};`}
`;

const StyledTextArea = styled(TextArea)`
    border: 0;
    ${(props) => props.inheritedFrom && `color: ${props.theme.global.colors['dark-6']};`}
`;

const TextInputField = ({
    help,
    label,
    inheritedFrom,
    color,
    error,
    name,
    area,
    type,
    min,
    max,
    readOnly,
    value,
    autofocus,
    disabled,
    ...rest
}) => {
    const Text = area ? StyledTextArea : StyledTextInput;

    //TODO: maybe move to AutoForm
    const ref = useAutofocus(autofocus);

    return (
        <Field label={label} help={help} inheritedFrom={inheritedFrom} color={color} error={error}>
            <Text
                name={name}
                size='medium'
                inheritedFrom={inheritedFrom}
                type={type}
                min={min}
                max={max}
                readOnly={readOnly}
                disabled={disabled}
                value={readOnly ? value : undefined}
                resize='vertical'
                ref={ref}
                {...rest}
            />
        </Field>
    );
};

TextInputField.propTypes = {
    ...InfoLabel.propTypes,
    name: PropTypes.string.isRequired,
    value: PropTypes.string,
    area: PropTypes.bool,
    readOnly: PropTypes.bool,
};

TextInputField.defaultProps = {
    ...InfoLabel.defaultProps,
    value: null,
    area: false,
    readOnly: false,
};

TextInputField.displayName = 'TextInputField';

export default TextInputField;
