import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Button, Text, Form, FormField, ThemeContext } from 'grommet';
import { AutoForm, TextField, ToggleField } from '@xtraplatform/core';

const NavLogin = ({ loginExpired, loginError, color, onLogin }) => {
    const { t } = useTranslation();

    const fieldsInitial = {
        rememberMe: false,
    };

    const login = (credentials) => {
        console.log('SIGNIN', credentials);
        onLogin(credentials);
        /*onLogin({ ...ui });
        setUI(initialUI);
        if (e && e.target && e.target.user) {
            e.target.user.focus();
        }*/
    };

    return (
        <Box flex='grow' justify='start' pad='medium'>
            {loginExpired && (
                <Box margin={{ bottom: 'medium' }}>
                    <Text color={color} weight='bold'>
                        {t('services/ogc_api:manager.sessionTimedOut._label')}
                    </Text>
                </Box>
            )}
            <AutoForm fields={fieldsInitial} onSubmit={login}>
                <TextField
                    name='user'
                    autoFocus={true}
                    label={<Text color={color}>{t('services/ogc_api:manager.user._label')}</Text>}
                />
                <TextField
                    name='password'
                    type='password'
                    label={
                        <Text color={color}>{t('services/ogc_api:manager.password._label')}</Text>
                    }
                    error={loginError && t(`services/ogc_api:manager.${loginError}._label`)}
                />
                <Box pad={{ vertical: 'medium', left: 'small' }}>
                    <ToggleField
                        name='rememberMe'
                        label={
                            <Text color={color}>
                                {t('services/ogc_api:manager.rememberMe._label')}
                            </Text>
                        }
                        smaller={true}
                        toggle={true}
                        reverse={true}
                    />
                </Box>
                <Box pad={{ vertical: 'medium' }}>
                    <Button
                        primary
                        label={t('services/ogc_api:manager.login._label')}
                        type='submit'
                    />
                </Box>
            </AutoForm>
        </Box>
    );
};

NavLogin.displayName = 'NavLogin';

export default NavLogin;
