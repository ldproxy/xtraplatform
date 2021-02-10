import React from 'react';
import PropTypes from 'prop-types';

import { Select } from 'grommet';
import InfoLabel from '../../InfoLabel';
import Field from '../Field';

const SelectField = ({
    inheritedFrom,
    name,
    options,
    placeholder,
    multiple,
    readOnly,
    disabled,
    value,
    clear,
    onClose,
    onSearch,
    ...rest
}) => {
    return (
        <Field inheritedFrom={inheritedFrom} {...rest}>
            <Select
                name={name}
                options={options}
                placeholder={placeholder}
                multiple={multiple}
                readOnly={readOnly}
                disabled={disabled}
                value={readOnly || disabled ? value : undefined}
                clear={clear}
                closeOnChange={!multiple}
                onClose={onClose}
                onSearch={onSearch}
            />
        </Field>
    );
};

SelectField.propTypes = {
    ...InfoLabel.propTypes,
    name: PropTypes.string.isRequired,
    value: PropTypes.string,
    multiple: PropTypes.bool,
    readOnly: PropTypes.bool,
    disabled: PropTypes.bool,
};

SelectField.defaultProps = {
    ...InfoLabel.defaultProps,
    value: null,
    multiple: false,
    readOnly: false,
    disabled: false,
};

SelectField.displayName = 'SelectField';

export default SelectField;
