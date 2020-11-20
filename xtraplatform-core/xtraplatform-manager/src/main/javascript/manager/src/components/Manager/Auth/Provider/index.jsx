import React from 'react';
import PropTypes from 'prop-types';

import AuthContext from '../Context';
import { useProvideAuth } from '../../../../hooks/auth';

// Provider component that wraps your app and makes auth object ...
// ... available to any child component that calls useAuth().
const AuthProvider = ({ baseUrl, allowAnonymousAccess, children }) => {
    const auth = useProvideAuth(baseUrl, allowAnonymousAccess);

    return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
};

AuthProvider.displayName = 'AuthProvider';

AuthProvider.propTypes = {
    baseUrl: PropTypes.string.isRequired,
    allowAnonymousAccess: PropTypes.bool,
    children: PropTypes.element.isRequired,
};

AuthProvider.defaultProps = {
    allowAnonymousAccess: false,
};

export default AuthProvider;
