import React, { useState } from 'react';
import { useFassets } from 'feature-u';
import { Grommet } from 'grommet';
import { HashRouter as Router, Switch, Route } from 'react-router-dom';
import { InMemoryCache, ApolloClient, ApolloProvider } from '@apollo/client';
import { RestLink } from 'apollo-link-rest';
import i18next from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import { AuthProvider } from '../Auth';
import { ViewProvider } from '../View';
import Layout from '../Layout';
import DefaultRoute from './DefaultRoute';
import { theme, routes, i18n, i18nAsResources } from '../../../feature-u';

const baseUrl = '../rest/admin';

const restLink = new RestLink({
    uri: baseUrl,
});

const client = new ApolloClient({
    link: restLink,
    cache: new InMemoryCache(),
});

const ManagerApp = () => {
    // TODO: set in ldproxy-manager (via fasset)
    const appName = 'ldproxy';
    const themeName = 'default';
    const themeMode = 'light';
    const secured = false;
    const isAdvanced = true;

    const [isLayerActive, setLayerActive] = useState(false);
    const closeLayer = () => {
        setLayerActive(false);
    };

    // TODO: role not available here, move routing to subcomponent, can use useAuth there
    const role = 'admin';
    const allowedRoutes = useFassets(routes())
        .flat(1)
        .filter((route) => !route.roles || route.roles.some((allowedRole) => allowedRole === role));

    if (process.env.NODE_ENV !== 'production') {
        console.log('ROUTES', allowedRoutes);
    }

    const menuRoutes = allowedRoutes.filter((route) => route.menuLabel);
    const defaultRoute = allowedRoutes.find((route) => route.default);
    const activeTheme = useFassets(theme(themeName));
    console.log('THEME', activeTheme);

    i18next
        .use(initReactI18next) // passes i18n down to react-i18next
        .use(LanguageDetector) // TODO: for user overrides, call i18next.changeLanguage after auth
        .init({
            //lng: 'en', //TODO: from browser
            fallbackLng: 'en',
            resources: i18nAsResources(useFassets(i18n())),
            detection: {
                order: ['navigator'],
            },
        });

    return (
        <AuthProvider baseUrl={baseUrl} allowAnonymousAccess={!secured}>
            <ViewProvider isAdvanced={isAdvanced}>
                <ApolloProvider client={client}>
                    <Grommet full theme={activeTheme} themeMode={themeMode}>
                        <Router>
                            <Switch>
                                <Route path='/' exact>
                                    <DefaultRoute defaultRoute={defaultRoute}>
                                        <Layout appName={appName} menuRoutes={menuRoutes} />
                                    </DefaultRoute>
                                </Route>
                                {allowedRoutes.map(({ path, content, sidebar }) => (
                                    <Route key={path} path={path} exact>
                                        <Layout
                                            appName={appName}
                                            menuRoutes={menuRoutes}
                                            sidebar={sidebar}
                                            content={content}
                                            isLayerActive={isLayerActive}
                                            closeLayer={closeLayer}
                                        />
                                    </Route>
                                ))}
                            </Switch>
                        </Router>
                    </Grommet>
                </ApolloProvider>
            </ViewProvider>
        </AuthProvider>
    );
};

ManagerApp.displayName = 'ManagerApp';

ManagerApp.propTypes = {};

export default ManagerApp;
