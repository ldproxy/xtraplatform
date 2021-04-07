import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Box, Button, Drop } from 'grommet';
import { CircleQuestion } from 'grommet-icons';
import { useHover } from '../../../hooks';

// needed for useHover to work correctly
const HoverIcon = styled(CircleQuestion)`
    pointer-events: none;
`;

const TooltipIcon = ({ icon, iconSize, iconColor, align, children }) => {
    const [hoverRef, isHovered] = useHover();

    return (
        <>
            <Button
                plain
                as='a'
                focusIndicator={false}
                icon={<HoverIcon as={icon} size={iconSize} color={iconColor} />}
                ref={hoverRef}
            />
            {isHovered && (
                <Drop align={align} target={hoverRef.current} plain>
                    <Box
                        margin='xsmall'
                        pad='xsmall'
                        width={{ max: 'medium' }}
                        background='background-front'
                        elevation='small'
                        round='small'
                        border={{
                            color: 'brand',
                            size: 'small',
                        }}
                        animation={['fadeIn']}>
                        {children}
                    </Box>
                </Drop>
            )}
        </>
    );
};

TooltipIcon.displayName = 'TooltipIcon';

TooltipIcon.propTypes = {
    icon: PropTypes.elementType.isRequired,
    iconSize: PropTypes.string,
    iconColor: PropTypes.string,
    align: PropTypes.object,
    children: PropTypes.element,
};

TooltipIcon.defaultProps = {
    iconSize: 'medium',
    iconColor: null,
    align: { top: 'bottom' },
    children: null,
};

export default TooltipIcon;
