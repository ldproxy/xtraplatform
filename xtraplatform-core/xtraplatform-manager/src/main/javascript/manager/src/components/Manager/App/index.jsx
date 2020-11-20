import React, { useState } from 'react';
import { useFassets } from 'feature-u';
import { Grommet } from 'grommet';
import { HashRouter as Router, Switch, Route } from 'react-router-dom';
import { InMemoryCache, ApolloClient, ApolloProvider } from '@apollo/client';
import { RestLink } from 'apollo-link-rest';

import { AuthProvider } from '../Auth';
import { ViewProvider } from '../View';
import Layout from '../Layout';
import DefaultRoute from './DefaultRoute';

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
    const activeTheme = 'default';
    const secured = false;
    const isAdvanced = true;

    const [isLayerActive, setLayerActive] = useState(false);
    const closeLayer = () => {
        setLayerActive(false);
    };

    // TODO: role not available here, move routing to subcomponent, can use useAuth there
    const role = 'admin';
    const routes = useFassets('*.routes')
        .flat(1)
        .filter((route) => !route.roles || route.roles.some((allowedRole) => allowedRole === role));

    if (process.env.NODE_ENV !== 'production') {
        console.log('ROUTES', routes);
    }

    const menuRoutes = routes.filter((route) => route.menuLabel);
    const defaultRoute = routes.find((route) => route.default);
    const theme = useFassets(`${activeTheme}.theme`);
    const themeMode = 'light';

    return (
        <AuthProvider baseUrl={baseUrl} allowAnonymousAccess={!secured}>
            <ViewProvider isAdvanced={isAdvanced}>
                <ApolloProvider client={client}>
                    <Grommet full theme={theme} themeMode={themeMode}>
                        <Router>
                            <Switch>
                                <Route path='/' exact>
                                    <DefaultRoute defaultRoute={defaultRoute}>
                                        <Layout appName={appName} menuRoutes={menuRoutes} />
                                    </DefaultRoute>
                                </Route>
                                {routes.map(({ path, content, sidebar }) => (
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
