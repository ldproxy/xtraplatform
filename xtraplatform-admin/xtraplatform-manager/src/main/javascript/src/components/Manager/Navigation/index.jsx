import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

import { Sidebar } from '../../Layout';
import NavigationHeader from './Header';
import NavigationMenu from './Menu';

const Navigation = (props) => {
    const { title, logo, routes, onClose, isLayer, isLayerActive, loginError, loginExpired, user, secured, onLogin, onLogout, onChangePassword } = props;
    const [isChangePassword, setChangePassword] = useState(false);

    if (isLayer && !isLayerActive) {
        return null;
    }

    if (process.env.NODE_ENV !== 'production') {
        console.log('USER', user);
    }

    return (
        <Sidebar isLayer={isLayer} hideBorder={true} onClose={onClose}>
            <Box fill="vertical" background="menu">
                <NavigationHeader isLayer={isLayer} onClose={onClose} title={title} logo={logo} />
                <NavigationMenu routes={routes} onClick={onClose} />
                {/*(!secured && !user)
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
                            : <NavLogin loginError={loginError} loginExpired={loginExpired} onLogin={onLogin} />)*/}
            </Box>
        </Sidebar>
    );
};

Navigation.displayName = 'Navigation';

Navigation.propTypes = {
    title: PropTypes.string,
    routes: PropTypes.array,
    onClose: PropTypes.func,
};

export default Navigation;
