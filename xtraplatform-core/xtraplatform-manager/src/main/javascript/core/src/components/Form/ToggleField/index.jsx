import React from 'react';
import PropTypes from 'prop-types';

import { FormField, CheckBox } from 'grommet';
import InfoLabel from '../../InfoLabel';

const ToggleField = ({ label, help, inheritedFrom, truthful, name, disabled }) => {
    return (
        <FormField>
            <CheckBox
                toggle
                name={name}
                inheritedFrom={inheritedFrom}
                label={
                    <InfoLabel
                        label={label}
                        help={help}
                        inheritedFrom={inheritedFrom}
                        color={truthful && !inheritedFrom ? 'inherit' : null}
                    />
                }
                disabled={disabled}
                onClick={(e) => e.target.blur()}
            />
        </FormField>
    );
};

ToggleField.propTypes = {
    ...InfoLabel.propTypes,
    name: PropTypes.string.isRequired,
    truthful: PropTypes.bool,
};

ToggleField.defaultProps = {
    ...InfoLabel.defaultProps,
    truthful: false,
};

ToggleField.displayName = 'ToggleField';

export default ToggleField;
