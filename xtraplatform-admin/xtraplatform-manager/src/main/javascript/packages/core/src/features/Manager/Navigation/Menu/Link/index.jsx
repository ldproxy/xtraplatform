import React from 'react';
import PropTypes from 'prop-types';
import { Link, useRouteMatch } from 'react-router-dom';
import styled from 'styled-components';

import { Box, Text } from 'grommet';
import { normalizeColor } from 'grommet/utils';

const StyledLink = styled(Link)`
    text-decoration: none;
    color: ${(props) =>
        normalizeColor(
            props.colorProp || props.theme.anchor.color,
            props.theme
        )};
`;

const StyledBox = styled(Box)`
    background-color: ${(props) =>
        props.isActive ? props.theme.menu.active.color : 'transparent'};

    &:hover {
        background-color: ${(props) => props.theme.menu.active.color};
    }
`;

const NavigationMenuLink = ({ path, label, onClick }) => {
    const match = useRouteMatch(path);
    // console.log(path, match, onClick);
    const isActive = match !== null;

    return (
        <Box focusIndicator={false} onClick={onClick}>
            <StyledLink to={path}>
                <StyledBox
                    isActive={isActive}
                    pad={{
                        left: 'medium',
                        right: 'xlarge',
                        vertical: 'small',
                    }}>
                    <Text>{label}</Text>
                </StyledBox>
            </StyledLink>
        </Box>
    );
};

NavigationMenuLink.displayName = 'NavigationMenuLink';

NavigationMenuLink.propTypes = {
    path: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onClick: PropTypes.func,
};

NavigationMenuLink.defaultProps = {
    onClick: null,
};

export default NavigationMenuLink;
