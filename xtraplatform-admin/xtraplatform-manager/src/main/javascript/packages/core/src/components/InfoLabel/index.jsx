import React from 'react';
import PropTypes from 'prop-types';
import styled from "styled-components";
import { useHover } from '../../hooks'

import { Box, Button, Drop, Text } from 'grommet';
import { CircleQuestion, Clone } from 'grommet-icons';

// needed for useHover to work correctly
const HelpIcon = styled(CircleQuestion)`
    pointer-events: none;
`
const InheritedIcon = styled(Clone)`
    pointer-events: none;
`
/**
 * InfoLabel can be used in FormField
 */
const InfoLabel = ({ label, help, inheritedFrom, ...rest }) => {
    const [helpHoverRef, isHelpHovered] = useHover();
    const [inheritedHoverRef, isInheritedHovered] = useHover();

    return (
        <Box direction="row" align="center" gap="small">
            {label}
            {inheritedFrom && <Button
                plain
                focusIndicator={false}
                icon={<InheritedIcon size="list" />}
                ref={inheritedHoverRef}
            />}
            {help && <Button
                plain
                focusIndicator={false}
                icon={<HelpIcon color="brand" size="medium" />}
                ref={helpHoverRef}
            />}
            {isInheritedHovered && (
                <Drop align={{ left: 'right' }} target={inheritedHoverRef.current} plain>
                    <Box
                        margin="xsmall"
                        pad="xsmall"
                        width={{ max: "large" }}
                        background="content"
                        elevation="small"
                        round="small"
                        border={{
                            color: "brand",
                            size: "small",
                        }}
                        animation={["fadeIn"]}
                    >
                        <Text size="small">{`Inherited from: ${inheritedFrom}`}</Text>
                    </Box>
                </Drop>
            )}
            {isHelpHovered && (
                <Drop align={{ left: 'right' }} target={helpHoverRef.current} plain>
                    <Box
                        margin="xsmall"
                        pad="xsmall"
                        width={{ max: "large" }}
                        background="content"
                        elevation="small"
                        round="small"
                        border={{
                            color: "brand",
                            size: "small",
                        }}
                        animation={["fadeIn"]}
                    >
                        <Text size="small">{help}</Text>
                    </Box>
                </Drop>
            )}
        </Box>
    );
};

InfoLabel.displayName = 'InfoLabel';

InfoLabel.propTypes = {
    /**
     * The label text
     */
    label: PropTypes.string.isRequired,
    /**
     * The text shown in the help tooltip
     */
    help: PropTypes.string,
    /**
     * The text shown in the inherited tooltip
     */
    inheritedFrom: PropTypes.string,
};

export default InfoLabel;
