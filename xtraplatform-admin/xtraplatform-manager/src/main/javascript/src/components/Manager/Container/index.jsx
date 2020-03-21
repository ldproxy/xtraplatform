import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';
import { Box, Grommet } from 'grommet';
import { BrowserRouter as Router, Route } from 'react-router-dom';

import Navigation from '../Navigation';
import Content from '../Content';

const Manager = () => {

  const appName = 'xtraplatform';
  const user = null;
  const role = 'admin';
  const activeTheme = 'default';

  const isLayer = false;
  const [isLayerActive, setLayerActive] = useState(true);
  const closeLayer = () => { setLayerActive(false) }

  const routes = useFassets('*.route').filter((route) => !route.roles || route.roles.some((allowedRole) => allowedRole === role));
  const routesInfo = routes.map(route => route.info);
  const theme = useFassets(`${activeTheme}.theme`);

  return (
    <Router>
      <Grommet full theme={theme}>
        <Box direction="row" fill>
          <Navigation title={appName} routes={routesInfo} isLayer={isLayer} isLayerActive={isLayerActive} onClose={closeLayer} />
          {routes.map(({ info, components }) => <Content key={info.path} path={info.path} Header={components.header} Main={components.main} />)}
        </Box>
      </Grommet>
    </Router>
  );
}

Manager.displayName = 'Manager';

Manager.propTypes = {
};

export default Manager;
