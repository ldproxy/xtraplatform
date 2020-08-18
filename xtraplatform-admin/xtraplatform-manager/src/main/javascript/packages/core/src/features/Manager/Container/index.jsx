import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';
import { Box, Grommet } from 'grommet';
import { HashRouter as Router, Switch, Route } from 'react-router-dom';
import { InMemoryCache, ApolloClient, ApolloProvider } from '@apollo/client'
import { RestLink } from 'apollo-link-rest'

import Navigation from '../Navigation';
import { Sidebar } from '../../../index';
import { ProvideAuth, useAuth } from '../../../auth'

const baseUrl = '../rest/admin';

const restLink = new RestLink({
    uri: baseUrl,
    //TODO
    /*headers: {
        Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJTVVBFUkFETUlOIiwicmVtZW1iZXJNZSI6ZmFsc2UsImV4cCI6MTc1MzE5MjU3NX0.mqgnhsZ7uH_cJp34a0Uh_97wymSOCMdOpgj_WtD49rY'
    },*/
})

const client = new ApolloClient({
    link: restLink,
    cache: new InMemoryCache()
})


const Manager = () => {

    //TODO: set in ldproxy-manager (via fasset)
    const appName = 'xtraplatform';
    const activeTheme = 'default';
    const secured = false;

    //TODO: get from jwt
    const user = null;
    const role = 'admin';

    const isLayer = false;
    const [isLayerActive, setLayerActive] = useState(false);
    const closeLayer = () => { setLayerActive(false) }


    const routes = useFassets('*.routes').flat(1).filter((route) => !route.roles || route.roles.some((allowedRole) => allowedRole === role));

    if (process.env.NODE_ENV !== 'production') {
        console.log('ROUTES', routes);
    }

    const menuRoutes = routes.filter(route => route.menuLabel);
    const theme = useFassets(`${activeTheme}.theme`);

    return (
        <ProvideAuth baseUrl={baseUrl} allowAnonymousAccess={!secured}>
            <ApolloProvider client={client}>
                <Grommet full theme={theme}>
                    <Router>
                        <Switch>
                            {routes.map(({ path, content, sidebar }) =>
                                <Route key={path} path={path} exact>
                                    <Box direction="row" fill>
                                        <Navigation title={appName} routes={menuRoutes} isLayer={!!sidebar} isLayerActive={isLayerActive} onClose={closeLayer} />
                                        {sidebar && <Sidebar>{sidebar}</Sidebar>}
                                        {content}
                                    </Box>
                                </Route>
                            )}
                        </Switch>
                    </Router>
                </Grommet>
            </ApolloProvider>
        </ProvideAuth>
    );
}

Manager.displayName = 'Manager';

Manager.propTypes = {
};

export default Manager;
