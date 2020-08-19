import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

import { Sidebar } from '../../Layout';
import NavigationHeader from './Header';
import NavigationMenu from './Menu';

const Navigation = ({
    title,
    logo,
    routes,
    onClose,
    isLayer,
    isLayerActive,
    user /* loginError, loginExpired, secured, onLogin, onLogout, onChangePassword, */,
}) => {
    // const [isChangePassword, setChangePassword] = useState(false);

    if (isLayer && !isLayerActive) {
        return null;
    }

    if (process.env.NODE_ENV !== 'production') {
        console.log('USER', user);
    }

    return (
        <Sidebar isLayer={isLayer} hideBorder onClose={onClose}>
            <Box fill='vertical' background='menu'>
                <NavigationHeader
                    isLayer={isLayer}
                    onClose={onClose}
                    title={title}
                    logo={logo}
                />
                <NavigationMenu routes={routes} onClick={onClose} />
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
    routes: PropTypes.arrayOf(),
    onClose: PropTypes.func,
    isLayer: PropTypes.bool,
    isLayerActive: PropTypes.bool,
    user: PropTypes.objectOf(),
};

Navigation.defaultProps = {
    title: null,
    logo: null,
    routes: [],
    onClose: null,
    isLayer: false,
    isLayerActive: false,
    user: null,
};

export default Navigation;
