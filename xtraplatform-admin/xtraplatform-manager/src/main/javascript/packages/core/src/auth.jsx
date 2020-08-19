import React, {
    useState,
    useEffect,
    useContext,
    useCallback,
    createContext,
} from 'react';
import PropTypes from 'prop-types';
import jwtDecode from 'jwt-decode';

const authContext = createContext();

// Hook for child components to get the auth object ...
// ... and re-render when it changes.
export const useAuth = () => {
    return useContext(authContext);
};

const getCookieValue = (name) => {
    console.log('COOKIES', document.cookie);
    const value = document.cookie.match(
        new RegExp(`(^|[^;]+)\\s*${name}\\s*=\\s*([^;]+)`, 'g')
    );
    console.log('COOKIE', value);
    return value ? value.pop() : '';
};

const deleteCookie = (name) => {
    const cookie = `${name}=; Max-Age=0; path=/`;
    document.cookie = cookie;
};

const TOKEN_COOKIE_NAME = 'xtraplatform-token';

const getToken = (baseUrl, credentials) => {
    return fetch(`${baseUrl}/../auth/token`, {
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        method: 'POST',
        body: JSON.stringify(credentials),
    }).then((response) => {
        try {
            const user = jwtDecode(getCookieValue(TOKEN_COOKIE_NAME));
            console.log('AUTH', user);
            if (user && user.role && user.role !== 'USER') {
                return {
                    user,
                    error: null,
                    expired: false,
                };
            }
        } catch (e) {
            console.log('NO AUTH', response);
        }

        return {
            user: null,
            error:
                response && response.error ? response.error : 'Not authorized',
            expired: false,
        };
    });
};

const clearToken = () => {
    deleteCookie(TOKEN_COOKIE_NAME);

    return {
        user: null,
        error: null,
    };
};

// Provider hook that creates auth object and handles state
const useProvideAuth = (baseUrl, allowAnonymousAccess) => {
    const [state, setState] = useState({});

    const signin = useCallback(
        (credentials) => {
            return getToken(baseUrl, credentials).then((token) => {
                setState(token);
                return token;
            });
        },
        [baseUrl]
    );

    const signout = useCallback(() => {
        return setState(clearToken());
    }, []);

    useEffect(() => {
        if (allowAnonymousAccess) {
            // eslint-disable-next-line no-inner-declarations
            async function waitForSignin() {
                await signin({ rememberMe: true });
            }
            waitForSignin();
        }
    }, [allowAnonymousAccess, signin]);

    // Return the user object and auth methods
    return {
        state,
        signin,
        signout,
    };
};

// Provider component that wraps your app and makes auth object ...
// ... available to any child component that calls useAuth().
export const ProvideAuth = ({ baseUrl, allowAnonymousAccess, children }) => {
    const auth = useProvideAuth(baseUrl, allowAnonymousAccess);
    return <authContext.Provider value={auth}>{children}</authContext.Provider>;
};

ProvideAuth.propTypes = {
    baseUrl: PropTypes.string.isRequired,
    allowAnonymousAccess: PropTypes.bool,
    children: PropTypes.element.isRequired,
};

ProvideAuth.defaultProps = {
    allowAnonymousAccess: false,
};
