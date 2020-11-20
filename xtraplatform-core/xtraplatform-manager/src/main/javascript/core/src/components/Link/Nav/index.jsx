import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';

import IconLink from '../Icon';

const IconLinkFromRouterLink = ({ navigate, ...rest }) => <IconLink {...rest} onClick={navigate} />;

const NavLink = ({ to, ...rest }) => {
    return <Link {...rest} to={to} component={IconLinkFromRouterLink} />;
};

NavLink.displayName = 'NavLink';

NavLink.propTypes = {
    to: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
    ...IconLink.propTypes,
};

export default NavLink;
