import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Box, Anchor } from 'grommet';

const StyledAnchor = styled(Anchor)`
    text-decoration: none;
    padding: 0;
    display: flex;
    ${(props) => props.iconSize && `height: ${props.theme.icon.size[props.iconSize]};`}
    &:hover {
        & svg {
            stroke: ${(props) => props.theme.global.colors.active};
        }
    }
`;

const IconLink = ({ icon, onClick, flex, pad, ...rest }) => {
    const iconSize = (icon && icon.props.size) || 'medium';

    return (
        <Box flex={flex} pad={pad}>
            <StyledAnchor
                {...rest}
                icon={icon}
                iconSize={iconSize}
                onClick={(event) => {
                    event.preventDefault();
                    onClick();
                    event.currentTarget.blur();
                }}
            />
        </Box>
    );
};

IconLink.displayName = 'IconLink';

IconLink.propTypes = {
    icon: PropTypes.element,
    flex: PropTypes.bool,
    pad: PropTypes.string,
    onClick: PropTypes.func,
};

IconLink.defaultProps = {
    icon: null,
    flex: false,
    pad: null,
    onClick: () => {},
};

export default IconLink;
