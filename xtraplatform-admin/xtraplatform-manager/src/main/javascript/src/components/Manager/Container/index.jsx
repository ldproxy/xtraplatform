import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';
import { Box, Grommet } from 'grommet';
import { BrowserRouter as Router, Switch, Route } from 'react-router-dom';

import Navigation from '../Navigation';
import Content from '../Content';

const Manager = () => {

    //TODO: set in ldproxy-manager (via props?)
    const appName = 'xtraplatform';
    const activeTheme = 'default';
    //TODO: get from jwt
    const user = null;
    const role = 'admin';

    const isLayer = false;
    const [isLayerActive, setLayerActive] = useState(true);
    const closeLayer = () => { setLayerActive(false) }

    const routes = useFassets('*.routes').flat(1).filter((route) => !route.roles || route.roles.some((allowedRole) => allowedRole === role));

    if (process.env.NODE_ENV !== 'production') {
        console.log('ROUTES', routes);
    }

    const menuRoutes = routes.filter(route => route.menuLabel);
    const theme = useFassets(`${activeTheme}.theme`);

    return (
        <Router>
            <Grommet full theme={theme}>
                <Box direction="row" fill>
                    <Navigation title={appName} routes={menuRoutes} isLayer={isLayer} isLayerActive={isLayerActive} onClose={closeLayer} />
                    <Switch>
                        {routes.map(({ path, headerComponent, mainComponent }) =>
                            <Route key={path} path={path} exact>
                                <Content Header={headerComponent} Main={mainComponent} />
                            </Route>
                        )}
                    </Switch>
                </Box>
            </Grommet>
        </Router>
    );
}

Manager.displayName = 'Manager';

Manager.propTypes = {
};

export default Manager;
