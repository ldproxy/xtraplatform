import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import { AutoForm, TextField, ToggleField, getFieldsDefault } from '@xtraplatform/core';

const ServiceEditGeneral = ({
    id,
    label,
    description,
    enabled,
    secured,
    defaults,
    isDefaults,
    inheritedLabel,
    debounce,
    onPending,
    onChange,
}) => {
    const fields = {
        label,
        description,
        enabled,
        secured,
    };
    const fieldsDefault = getFieldsDefault(fields, defaults);

    return (
        <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill='horizontal'>
            <AutoForm
                fields={fields}
                fieldsDefault={fieldsDefault}
                inheritedLabel={inheritedLabel}
                debounce={debounce}
                onPending={onPending}
                onChange={onChange}>
                {isDefaults || <TextField name='id' label='Id' help='TODO' value={id} readOnly />}
                <TextField name='label' label='Label' help='TODO' />
                <TextField area name='description' label='Description' help='TODO' />
                {isDefaults && <ToggleField name='enabled' label='Enable service' help='TODO' />}
                <ToggleField name='secured' label='Restrict write access' help='TODO' />
            </AutoForm>
        </Box>
    );
};

ServiceEditGeneral.displayName = 'ServiceEditGeneral';

ServiceEditGeneral.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string,
    description: PropTypes.string,
    isDefaults: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
};

export default ServiceEditGeneral;
