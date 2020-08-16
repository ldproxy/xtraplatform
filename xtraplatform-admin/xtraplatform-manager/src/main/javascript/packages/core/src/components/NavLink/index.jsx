import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom'
import styled from "styled-components";

import { Anchor } from 'grommet';

const StyledAnchor = styled(Anchor)`
    text-decoration: none;
    padding: 0;
    ${props => props.iconSize &&
        `height: ${props.theme.icon.size[props.iconSize]};`}
`;

const LinkAnchor = props => {
    const iconSize = (props.icon && props.icon.props.size) || 'medium';

    return (<StyledAnchor {...props} iconSize={iconSize} onClick={event => {
        event.preventDefault();
        props.navigate();
        event.currentTarget.blur();
    }} />)
}

const NavLink = ({ to, ...rest }) => {
    return (
        <Link {...rest} to={to} component={LinkAnchor} />
    );
};

NavLink.displayName = 'NavLink';

NavLink.propTypes = {
    to: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired
};

export default NavLink;
