import React, { useEffect } from 'react';
import { Box } from 'grommet';

import { Sidebar } from '@xtraplatform/core';
import Navigation from '../Navigation';
import { useAuth } from '../../../hooks/auth';
import { useHistory } from 'react-router-dom';
import { useView } from '../../../hooks/view';

const ManagerLayout = ({ appName, menuRoutes, sidebar, content }) => {
    const [auth] = useAuth();
    const [{ isMenuOpen }, { toggleMenu }] = useView();
    const history = useHistory();

    const isLoggedIn = !!auth.user && !auth.user.forceChangePassword;
    const isLoggingIn = !!auth.loading || (!!auth.user && !!auth.user.forceChangePassword);
    const isNotAuthorized = !!auth.error;

    if (process.env.NODE_ENV !== 'production') {
        if (isNotAuthorized) console.log('redirecting to /');
        if (isLoggingIn) console.log('logging in');
        if (isLoggedIn) console.log('logged in', auth.user);
    }

    useEffect(() => {
        if (isNotAuthorized) {
            history.push('/');
        }
    }, [isNotAuthorized, history]);

    return (
        <Box direction='row' fill>
            <Navigation
                title={appName}
                routes={menuRoutes}
                isLayer={!!sidebar}
                isLayerActive={isMenuOpen}
                onClose={toggleMenu}
            />
            {isLoggedIn && sidebar && <Sidebar>{sidebar}</Sidebar>}
            {isLoggedIn && content}
        </Box>
    );
};

ManagerLayout.displayName = 'ManagerLayout';

ManagerLayout.propTypes = {};

export default ManagerLayout;
