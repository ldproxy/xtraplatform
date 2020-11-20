import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { Box, Button, Text, Form, FormField, ThemeContext } from 'grommet';

import TextInputUi from '../common/TextInputUi';

import { validator, minLength, maxLength, equals, differs } from 'xtraplatform-manager/src/components/common/ui-validator';

const v = validator({
    oldPassword: [minLength(1), maxLength(64)],
    newPassword: [minLength(6), maxLength(64), differs('oldPassword', 'old password')],
    newPasswordRepeat: equals('newPassword', 'new password')
})

const NavChangePassword = ({ name, onChange, onCancel }) => {
    const initialUI = {
        user: name,
        oldPassword: '',
        newPassword: '',
        newPasswordRepeat: '',
        error: null
    }
    const [ui, setUI] = useState(initialUI);
    const updateUI = (name, value) => setUI({
        ...ui,
        [name]: value
    })

    const vldtr = v(ui);

    const reset = (e) => {
        onCancel && onCancel();
        setUI(initialUI)
        if (e && e.target && e.target.oldPassword) {
            e.target.oldPassword.focus();
        }
    }

    const submit = (e) => {
        if (vldtr.valid) {
            onChange({ ...ui });
            reset(e);
        }
    }

    return (
        <ThemeContext.Extend
            value={{
                formField: {
                    border: {
                        position: 'outer',
                        side: 'bottom',
                        size: 'small',
                        color: 'dark-1'
                    },
                    extend: {
                        background: 'light-6'
                    }
                }
            }}
        >
            <Box flex='grow' justify='start' pad="medium">
                <Box margin={{ bottom: 'medium' }}><Text color="white" weight="bold">Change password for user '{name}'</Text></Box>
                <Form onSubmit={submit} onReset={reset}>
                    <FormField label={<Text color="light-2">Old password</Text>} background="light-4" error={vldtr.messages.oldPassword}>
                        <TextInputUi name="oldPassword"
                            autoFocus={true}
                            type="password"
                            value={ui.oldPassword}
                            onChange={updateUI} />
                    </FormField>
                    <FormField label={<Text color="light-2">New password</Text>} error={vldtr.messages.newPassword} >
                        <TextInputUi name="newPassword"
                            type="password"
                            value={ui.newPassword}
                            onChange={updateUI} />
                    </FormField>
                    <FormField label={<Text color="light-2">Repeat new password</Text>} error={vldtr.messages.newPasswordRepeat} >
                        <TextInputUi name="newPasswordRepeat"
                            type="password"
                            value={ui.newPasswordRepeat}
                            onChange={updateUI} />
                    </FormField>
                    <Box pad={{ vertical: 'medium' }} direction="row" justify="around">
                        {onCancel && <Button label="Cancel" type="reset" />}
                        <Button primary label="Submit" type="submit" />
                    </Box>
                </Form>
            </Box>
        </ThemeContext.Extend>
    );
}

NavChangePassword.displayName = 'NavChangePassword';

export default NavChangePassword
