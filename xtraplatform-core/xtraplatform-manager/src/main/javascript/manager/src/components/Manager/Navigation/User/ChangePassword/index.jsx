import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box, Button, Text } from 'grommet';
import { AutoForm, TextField, minLength, maxLength, equals, differs } from '@xtraplatform/core';

const fieldsValidation = {
    oldPassword: [minLength(1), maxLength(64)],
    newPassword: [minLength(6), maxLength(64), differs('oldPassword', 'old password')],
    newPasswordRepeat: equals('newPassword', 'new password'),
};

const NavChangePassword = ({ name, disabled, onChange, onCancel }) => {
    const { t } = useTranslation();

    const fieldsInitial = {
        user: name,
    };

    const submit = (change, reset) => {
        console.log('SUBMIT', change);
        reset();
        onChange(change);
    };

    return (
        <Box flex='grow' justify='start' pad='medium'>
            <Box margin={{ bottom: 'medium' }}>
                <Text color='white' weight='bold'>
                    {t('services/ogc_api:manager.changePassword._description', { user: name })}
                </Text>
            </Box>
            <AutoForm
                fields={fieldsInitial}
                fieldsValidation={fieldsValidation}
                onSubmit={submit}
                onCancel={onCancel}>
                <TextField
                    name='oldPassword'
                    autoFocus={true}
                    type='password'
                    label={
                        <Text color='light-2'>
                            {t('services/ogc_api:manager.oldPassword._label')}
                        </Text>
                    }
                    background='light-4'
                    disabled={disabled}
                />
                <TextField
                    name='newPassword'
                    type='password'
                    label={
                        <Text color='light-2'>
                            {t('services/ogc_api:manager.newPassword._label')}
                        </Text>
                    }
                    disabled={disabled}
                />
                <TextField
                    name='newPasswordRepeat'
                    type='password'
                    label={
                        <Text color='light-2'>
                            {t('services/ogc_api:manager.repeatNewPassword._label')}
                        </Text>
                    }
                    disabled={disabled}
                />
                <Box pad={{ vertical: 'medium' }} gap='medium'>
                    {onCancel && (
                        <Button
                            label={t('services/ogc_api:manager.cancel._label')}
                            type='reset'
                            disabled={disabled}
                        />
                    )}
                    <Button
                        primary
                        label={t('services/ogc_api:manager.submit._label')}
                        type='submit'
                        disabled={disabled}
                    />
                </Box>
            </AutoForm>
        </Box>
    );
};

NavChangePassword.displayName = 'NavChangePassword';

export default NavChangePassword;
