import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { Box, Button, Text, Form, FormField, ThemeContext } from 'grommet';

import TextInputUi from '../common/TextInputUi';
import CheckBoxUi from '../common/CheckboxUi';


const NavLogin = ({ loginExpired, loginError, onLogin }) => {
    const initialUI = {
        user: '',
        password: '',
        rememberMe: false
    }
    const [ui, setUI] = useState(initialUI);
    const updateUI = (name, value) => setUI({
        ...ui,
        [name]: value
    })

    const login = (e) => {
        onLogin({ ...ui });
        setUI(initialUI)
        if (e && e.target && e.target.user) {
            e.target.user.focus();
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
                {loginExpired && <Box margin={{ bottom: 'medium' }}><Text color="white" weight="bold">Session timed out</Text></Box>}
                <Form onSubmit={login}>
                    <FormField label={<Text color="light-2">User</Text>} background="light-4">
                        <TextInputUi name="user"
                            autoFocus={true}
                            value={ui.user}
                            onChange={updateUI} />
                    </FormField>
                    <FormField label={<Text color="light-2">Password</Text>} error={loginError}>
                        <TextInputUi name="password"
                            type="password"
                            value={ui.password}
                            onChange={updateUI} />
                    </FormField>
                    <Box pad={{ vertical: 'medium', left: 'small' }}>
                        <CheckBoxUi name="rememberMe"
                            label={<Text color="light-2">Remember me</Text>}
                            checked={ui.rememberMe}
                            smaller={true}
                            toggle={true}
                            reverse={true}
                            onChange={updateUI} />
                    </Box>
                    <Box pad={{ vertical: 'medium' }}>
                        <Button primary label="Login" type="submit" />
                    </Box>
                </Form>
            </Box>
        </ThemeContext.Extend>
    );
}

NavLogin.displayName = 'NavLogin';

export default NavLogin
