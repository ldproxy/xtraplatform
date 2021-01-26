import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Form } from 'grommet';
import {
    AutoForm,
    TextField,
    ToggleField,
    RadioField,
    required,
    url,
    ifEqualsThen,
} from '@xtraplatform/core';

export const fieldsInitial = {
    featureProviderType: 'SQL',
};

export const fieldsValidation = {
    host: ifEqualsThen('featureProviderType', 'SQL', required()),
    database: ifEqualsThen('featureProviderType', 'SQL', required()),
    user: ifEqualsThen('featureProviderType', 'SQL', required()),
    password: ifEqualsThen('featureProviderType', 'SQL', required()),
    url: ifEqualsThen('featureProviderType', 'WFS', url()),
};

//TODO: providers from fassets
const ServiceAddOgcApi = ({ isBasicAuth, loading, errors, featureProviderType }) => {
    const { t } = useTranslation();

    return (
        <>
            <RadioField
                name='featureProviderType'
                label={t('services/ogc_api:services.add.type._label')}
                options={['SQL', 'WFS']}
                disabled={loading}
            />
            {featureProviderType === 'SQL' && (
                <>
                    <TextField
                        name='host'
                        label={t('services/ogc_api:services.add.host._label')}
                        help={t('services/ogc_api:services.add.host._description')}
                        disabled={loading}
                        error={errors['host']}
                    />
                    <TextField
                        name='database'
                        label={t('services/ogc_api:services.add.database._label')}
                        help={t('services/ogc_api:services.add.database._description')}
                        disabled={loading}
                        error={errors['database']}
                    />
                    <TextField
                        name='user'
                        label={t('services/ogc_api:services.add.user._label')}
                        help={t('services/ogc_api:services.add.user._description')}
                        disabled={loading}
                        error={errors['user']}
                    />
                    <TextField
                        name='password'
                        label={t('services/ogc_api:services.add.password._label')}
                        help={t('services/ogc_api:services.add.password._description')}
                        disabled={loading}
                        error={errors['password']}
                        type='password'
                    />
                    <TextField
                        name='schemas'
                        label={t('services/ogc_api:services.add.schemas._label')}
                        help={t('services/ogc_api:services.add.schemas._description')}
                        disabled={loading}
                    />
                </>
            )}
            {featureProviderType === 'WFS' && (
                <>
                    <TextField
                        name='url'
                        label={t('services/ogc_api:services.add.url._label')}
                        help={t('services/ogc_api:services.add.url._description')}
                        disabled={loading}
                        error={errors['url']}
                    />
                    <ToggleField
                        name='isBasicAuth'
                        label={t('services/ogc_api:services.add.isBasicAuth._label')}
                        help={t('services/ogc_api:services.add.isBasicAuth._description')}
                        truthful={isBasicAuth}
                        disabled={loading}
                    />
                    {isBasicAuth && (
                        <TextField
                            name='user'
                            label={t('services/ogc_api:services.add.basicAuthUser._label')}
                            help={t('services/ogc_api:services.add.basicAuthUser._description')}
                            disabled={loading}
                        />
                    )}
                    {isBasicAuth && (
                        <TextField
                            name='password'
                            label={t('services/ogc_api:services.add.basicAuthPassword._label')}
                            help={t('services/ogc_api:services.add.basicAuthPassword._description')}
                            type='password'
                            disabled={loading}
                        />
                    )}
                </>
            )}
        </>
    );
};

ServiceAddOgcApi.displayName = 'ServiceAddOgcApi';

ServiceAddOgcApi.propTypes = {};

export default ServiceAddOgcApi;
