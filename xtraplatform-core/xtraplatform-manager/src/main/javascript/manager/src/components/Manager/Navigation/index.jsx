import React, { useState, useContext } from 'react';
import PropTypes from 'prop-types';

import { Box, ThemeContext } from 'grommet';

import { Sidebar } from '@xtraplatform/core';
import NavigationHeader from './Header';
import NavigationMenu from './Menu';
import { Login, ChangePassword, UserActions } from './User';
import { useAuth } from '../../../hooks/auth';
import { useChangePassword } from '../../../hooks/api';

const Navigation = ({ title, logo, routes, onClose, isLayer, isLayerActive }) => {
    const [isChangePassword, setChangePassword] = useState(false);
    const [auth, signin, signout, refresh] = useAuth();
    const { user, error, expired } = auth;
    const [changePassword, { loading: isChanging }] = useChangePassword(user && user.sub);
    const theme = useContext(ThemeContext);
    const color = theme.normalizeColor(theme.navigation.color, theme.navigation.dark);
    const bgColor = theme.navigation.background;
    

    if (isLayer && !isLayerActive) {
        return null;
    }

    const onChangePassword = (update) => {
        setChangePassword(false);
        changePassword(update).then(refresh);
    };

    return (
        <Sidebar isLayer={isLayer} hideBorder onClose={onClose}>
            <Box fill='vertical' background={bgColor} color={color}>
                <NavigationHeader isLayer={isLayer} onClose={onClose} title={title} logo={logo} color={color} />
                {user ? (
                    user.forceChangePassword || isChangePassword ? (
                        <ChangePassword
                            name={user.sub}
                            disabled={isChanging} 
                            color={color}
                            onCancel={!user.forceChangePassword && (() => setChangePassword(false))}
                            onChange={onChangePassword}
                        />
                    ) : (
                        <Box justify='around' fill='vertical'>
                            <NavigationMenu routes={routes} onClick={onClose} />
                            <UserActions
                                name={user.sub}
                                onLogout={signout}
                                onChangePassword={() => setChangePassword(true)}
                            />
                        </Box>
                    )
                ) : (
                    <Login loginError={error} loginExpired={expired} color={color} onLogin={signin} />
                )}
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
