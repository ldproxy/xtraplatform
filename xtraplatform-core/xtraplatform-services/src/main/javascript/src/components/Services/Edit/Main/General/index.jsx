import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box } from 'grommet';
import { AutoForm, TextField, ToggleField, getFieldsDefault } from '@xtraplatform/core';

const ServiceEditGeneral = ({
    id,
    label,
    description,
    enabled,
    secured,
    apiVersion,
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
        apiVersion,
    };
    const fieldsDefault = getFieldsDefault(fields, defaults);

    const { t } = useTranslation();

    return (
        <Box pad={{ horizontal: 'small', vertical: 'medium' }} fill='horizontal'>
            <AutoForm
                key={id}
                fields={fields}
                fieldsDefault={fieldsDefault}
                inheritedLabel={inheritedLabel}
                debounce={debounce}
                onPending={onPending}
                onChange={onChange}>
                {isDefaults || (
                    <TextField
                        name='id'
                        label={t('services/ogc_api:id._label')}
                        help={t('services/ogc_api:id._description')}
                        value={id}
                        readOnly
                    />
                )}
                <TextField
                    name='label'
                    label={t('services/ogc_api:label._label')}
                    help={t('services/ogc_api:label._description')}
                />
                <TextField
                    area
                    name='description'
                    label={t('services/ogc_api:description._label')}
                    help={t('services/ogc_api:description._description')}
                />
                <TextField
                    name='apiVersion'
                    label={t('services/ogc_api:apiVersion._label')}
                    help={t('services/ogc_api:apiVersion._description')}
                    type='number'
                    min='0'
                />
                {isDefaults && (
                    <ToggleField
                        name='enabled'
                        label={t('services/ogc_api:enabled._label')}
                        help={t('services/ogc_api:enabled._description')}
                    />
                )}
                {/* <ToggleField name='secured' label='Restrict write access' help='TODO' /> */}
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
