import React from 'react';
import PropTypes from 'prop-types';
import { Redirect } from 'react-router-dom';

import { useAuth } from '../../../../hooks';

const DefaultRoute = ({ defaultRoute, children }) => {
    const [auth] = useAuth();
    const isLoggedIn = !!auth.user;

    return defaultRoute && defaultRoute.path && isLoggedIn ? (
        <Redirect to={defaultRoute.path} />
    ) : (
        children
    );
};

DefaultRoute.propTypes = {
    defaultRoute: PropTypes.shape({ path: PropTypes.string }),
    children: PropTypes.element,
};

DefaultRoute.defaultProps = {
    defaultRoute: null,
    children: null,
};

DefaultRoute.displayName = 'DefaultRoute';

export default DefaultRoute;
