import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

import { Sidebar } from '@xtraplatform/core';
import NavigationHeader from './Header';
import NavigationMenu from './Menu';
import { UserActions } from './User';
import { useAuth } from '../../../hooks/auth';

const Navigation = ({
    title,
    logo,
    routes,
    onClose,
    isLayer,
    isLayerActive,
    /*user loginError, loginExpired, secured, onLogin, onLogout, onChangePassword, */
}) => {
    // const [isChangePassword, setChangePassword] = useState(false);
    const [auth, signin, signout] = useAuth();
    const { user, error } = auth;

    if (isLayer && !isLayerActive) {
        return null;
    }

    return (
        <Sidebar isLayer={isLayer} hideBorder onClose={onClose}>
            <Box fill='vertical' background='navigation'>
                <NavigationHeader isLayer={isLayer} onClose={onClose} title={title} logo={logo} />
                <NavigationMenu routes={routes} onClick={onClose} />
                {user && <UserActions name={user.sub} />}
                {/* (!secured && !user)
                    ? <></>
                    : (user
                        ? user.forceChangePassword || isChangePassword
                            ? <NavChangePassword name={user.sub} onCancel={!user.forceChangePassword && (() => setChangePassword(false))} onChange={(update) => { setChangePassword(false); onChangePassword(update); }} />
                            : (
                                <Box justify="around" fill="vertical">
                                    <NavMenu routes={routes} role={user.role} onClose={onClose} />
                                    <NavUser name={user.sub} onChangePassword={secured && (() => setChangePassword(true))} onLogout={secured && onLogout} />
                                </Box>
                            )
                            : <NavLogin loginError={loginError} loginExpired={loginExpired} onLogin={onLogin} />) */}
            </Box>
        </Sidebar>
    );
};

Navigation.displayName = 'Navigation';

Navigation.propTypes = {
    title: PropTypes.string,
    logo: PropTypes.string,
    routes: PropTypes.arrayOf(PropTypes.object),
    onClose: PropTypes.func,
    isLayer: PropTypes.bool,
    isLayerActive: PropTypes.bool,
};

Navigation.defaultProps = {
    title: null,
    logo: null,
    routes: [],
    onClose: null,
    isLayer: false,
    isLayerActive: false,
};

export default Navigation;
