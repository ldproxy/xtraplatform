import React, { useState } from 'react';
import PropTypes from 'prop-types';

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
    return (
        <>
            <RadioField
                name='featureProviderType'
                label='Data Source Type'
                help='TODO'
                options={['SQL', 'WFS']}
                disabled={loading}
            />
            {featureProviderType === 'SQL' && (
                <>
                    <TextField
                        name='host'
                        label='Host'
                        help='TODO'
                        disabled={loading}
                        error={errors['host']}
                    />
                    <TextField
                        name='database'
                        label='Database'
                        help='TODO'
                        disabled={loading}
                        error={errors['database']}
                    />
                    <TextField
                        name='user'
                        label='User'
                        help='TODO'
                        disabled={loading}
                        error={errors['user']}
                    />
                    <TextField
                        name='password'
                        label='Password'
                        help='TODO'
                        disabled={loading}
                        error={errors['password']}
                    />
                    <TextField
                        name='schemas'
                        label='Additional schemas'
                        help='TODO'
                        disabled={loading}
                    />
                </>
            )}
            {featureProviderType === 'WFS' && (
                <>
                    <TextField
                        name='url'
                        label='WFS URL'
                        help='The GetCapabilities endpoint of the existing service'
                        disabled={loading}
                        error={errors['url']}
                    />
                    <ToggleField
                        name='isBasicAuth'
                        label='Basic Auth'
                        help='Is the WFS secured with HTTP Basic Authentication?'
                        truthful={isBasicAuth}
                        disabled={loading}
                    />
                    {isBasicAuth && (
                        <TextField
                            name='user'
                            label='User'
                            help='The HTTP Basic Authentication user name'
                            disabled={loading}
                        />
                    )}
                    {isBasicAuth && (
                        <TextField
                            name='password'
                            label='Password'
                            help='The HTTP Basic Authentication password'
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
