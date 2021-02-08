import React, { useState, useEffect, useContext, useCallback } from 'react';
import jwtDecode from 'jwt-decode';

import { AuthContext } from '../components/Manager/Auth';

const getCookieValue = (name) => {
    // eslint-disable-next-line no-undef
    console.log('COOKIES', document.cookie);
    // eslint-disable-next-line no-undef
    const value = document.cookie.match(new RegExp(`(^|[^;]+)\\s*${name}\\s*=\\s*([^;]+)`, 'g'));
    console.log('COOKIE', value);
    return value ? value.pop() : '';
};

const deleteCookie = (name) => {
    const cookie = `${name}=; Max-Age=0; path=/`;
    // eslint-disable-next-line no-undef
    document.cookie = cookie;
};

const TOKEN_COOKIE_NAME = 'xtraplatform-token';

const getToken = (baseUrl, credentials) => {
    // eslint-disable-next-line no-undef
    return fetch(`${baseUrl}/../auth/token`, {
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        method: 'POST',
        body: JSON.stringify(credentials),
    }).then(parseToken);
};

const parseToken = (response) => {
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

    return response && !response.ok
        ? response.text().then((text) => {
              return {
                  user: null,
                  error: text || 'Not authorized',
                  expired: false,
              };
          })
        : {
              user: null,
              error: 'Not authorized',
              expired: false,
          };
};

const clearToken = () => {
    deleteCookie(TOKEN_COOKIE_NAME);

    return {
        user: null,
        error: null,
    };
};

// Hook for child components to get the auth object ...
// ... and re-render when it changes.
export const useAuth = () => {
    return useContext(AuthContext);
};

// Provider hook that creates auth object and handles state
export const useProvideAuth = (baseUrl, allowAnonymousAccess) => {
    const [state, setState] = useState({});

    const signin = useCallback(
        (credentials, ignoreErrors) => {
            setState({ loading: true });
            return getToken(baseUrl, credentials).then((token) => {
                const token2 = ignoreErrors ? { ...token, error: null, ignoredError: true } : token;
                setState(token2);
                return token2;
            });
        },
        [baseUrl]
    );

    const signout = useCallback((user) => {
        if (process.env.NODE_ENV !== 'production') {
            console.log('signing out', user);
        }
        return setState(clearToken());
    }, []);

    const refresh = useCallback(() => {
        if (process.env.NODE_ENV !== 'production') {
            console.log('reparsing token');
        }
        return setState(parseToken());
    }, []);

    useEffect(() => {
        if (!state.user && !state.loading && !state.error && !state.ignoredError) {
            const token = parseToken();
            // try anonymous signin, only once
            if (token.error /*&& allowAnonymousAccess*/) {
                // eslint-disable-next-line no-inner-declarations
                async function waitForSignin() {
                    const auth = await signin({ rememberMe: true }, true);
                }
                waitForSignin();
            } else {
                setState(token);
            }
        }
    }, [allowAnonymousAccess, signin, state]);

    // Return the user object and auth methods
    return [state, signin, signout, refresh];
};
