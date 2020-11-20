import React from 'react';
import PropTypes from 'prop-types';

import { FormField as FormFieldGrommet } from 'grommet';
import InfoLabel from '../../InfoLabel';

const FormField = ({ label, help, inheritedFrom, color, error, children }) => {
    return (
        <FormFieldGrommet
            error={error}
            label={
                <InfoLabel label={label} help={help} inheritedFrom={inheritedFrom} color={color} />
            }>
            {children}
        </FormFieldGrommet>
    );
};

FormField.propTypes = {
    ...InfoLabel.propTypes,
    children: PropTypes.element.isRequired,
};

FormField.defaultProps = {
    ...InfoLabel.defaultProps,
};

FormField.displayName = 'FormField';

export default FormField;
