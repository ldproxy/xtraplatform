import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import Link from './Link'

const NavigationMenu = ({ routes, onClick }) => {
    return (
        <Box flex="grow" justify="start">
            {routes.map((route) => (
                <Link key={route.path} path={route.path} label={route.menuLabel} onClick={onClick} />
            ))}
        </Box>
    );
};

NavigationMenu.displayName = 'NavigationMenu';

NavigationMenu.propTypes = {
    routes: PropTypes.arrayOf(PropTypes.object),
    onClick: PropTypes.func,
};

export default NavigationMenu;
