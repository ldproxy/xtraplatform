import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box } from 'grommet';
import {
    TextField,
    ToggleField,
    RadioField,
    FileField,
    required,
    url,
    ifEqualsThen,
    allowedChars,
} from '@xtraplatform/core';

const maxSize = 104857600;
export const fieldsInitial = {
    featureProviderType: 'SQL',
};

export const fieldsValidation = {
    host: ifEqualsThen('featureProviderType', 'SQL', required()),
    database: ifEqualsThen('featureProviderType', 'SQL', required()),
    user: ifEqualsThen('featureProviderType', 'SQL', required()),
    password: ifEqualsThen('featureProviderType', 'SQL', required()),
    url: ifEqualsThen('featureProviderType', 'WFS', url()),
    schemas: allowedChars('A-Za-z0-9-_,'),
    autoTypes: allowedChars('A-Za-z0-9-_,\\[\\]\\(\\)\\|\\*\\.\\{\\}'),
    file: (files) => {
        if (files && files[0] && files[0].size > maxSize) {
            return 'File is too large. Select file which is not larger than 100 MB.';
        }
    },
};

//TODO: providers from fassets
const ServiceAddOgcApi = ({ isBasicAuth, loading, errors, featureProviderType }) => {
    const { t } = useTranslation();
    return (
        <>
            <RadioField
                name='featureProviderType'
                label={t('services/ogc_api:services.add.type._label')}
                options={[
                    { value: 'SQL', label: 'PostgreSQL' },
                    { value: 'GPKG', label: 'GeoPackage' },
                    { value: 'WFS', label: 'WFS' },
                ]}
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
                        error={errors['schemas']}
                    />
                    <TextField
                        name='autoTypes'
                        label={t('services/ogc_api:services.add.autoTypes._label')}
                        help={t('services/ogc_api:services.add.autoTypes._description')}
                        disabled={loading}
                        error={errors['autoTypes']}
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

            {featureProviderType === 'GPKG' && (
                <>
                    <FileField
                        label={t('services/ogc_api:services.add.geoPackage._label')}
                        help={t('services/ogc_api:services.add.geoPackage._description')}
                        maxSize={maxSize}
                        error={errors['file']}
                        name='file'
                        accept='.gpkg'
                        multiple={false}
                    />
                    <Box
                        border={{ side: 'bottom', size: 'xsmall' }}
                        margin={{ vertical: 'small' }}
                    />
                    <TextField
                        name='schemas'
                        label={t('services/ogc_api:services.add.schemas._label')}
                        help={t('services/ogc_api:services.add.schemas._description')}
                        disabled={loading}
                        error={errors['schemas']}
                    />
                    <TextField
                        name='autoTypes'
                        label={t('services/ogc_api:services.add.autoTypes._label')}
                        help={t('services/ogc_api:services.add.autoTypes._description')}
                        disabled={loading}
                        error={errors['autoTypes']}
                    />
                </>
            )}
        </>
    );
};

ServiceAddOgcApi.displayName = 'ServiceAddOgcApi';

ServiceAddOgcApi.propTypes = {};

export default ServiceAddOgcApi;
