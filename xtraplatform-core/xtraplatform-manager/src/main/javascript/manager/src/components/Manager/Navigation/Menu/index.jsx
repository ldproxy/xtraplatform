import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Box } from 'grommet';
import Link from './Link';

const NavigationMenu = ({ routes, onClick }) => {
    const { t } = useTranslation();

    return (
        <Box flex='grow' justify='start'>
            {routes.map((route) => (
                // eslint-disable-next-line jsx-a11y/anchor-is-valid
                <Link
                    key={route.path}
                    path={route.path}
                    label={t(route.menuLabel)}
                    onClick={onClick}
                />
            ))}
        </Box>
    );
};

NavigationMenu.displayName = 'NavigationMenu';

NavigationMenu.propTypes = {
    routes: PropTypes.arrayOf(PropTypes.object).isRequired,
    onClick: PropTypes.func,
};

NavigationMenu.defaultProps = {
    onClick: null,
};

export default NavigationMenu;
