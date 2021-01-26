import React from 'react';
import PropTypes from 'prop-types';

import { Box, Text, Markdown } from 'grommet';
import { CircleQuestion, Clone } from 'grommet-icons';
import TooltipIcon from './TooltipIcon';

/**
 * InfoLabel can be used in FormField
 */
const InfoLabel = ({ label, help, inheritedFrom, iconSize, color, hoverColor, mono, boxProps }) => {
    const textProps = mono
        ? {
              size: 'small',
              weight: 'bold',
              color: color || 'dark-4',
              style: { fontFamily: '"Roboto Mono", monospace' },
          }
        : {};
    iconSize = mono ? 'list' : iconSize;

    return (
        <Box direction='row' align='center' gap='xsmall' {...boxProps}>
            <Text {...textProps}>{label}</Text>
            {inheritedFrom && (
                <TooltipIcon icon={Clone} iconSize={iconSize} iconColor='text'>
                    <Text size='small'>
                        Inherited from: <strong>{inheritedFrom}</strong>
                    </Text>
                </TooltipIcon>
            )}
            {help && (
                <TooltipIcon icon={CircleQuestion} iconSize={iconSize} iconColor='brand'>
                    <Text size='small'>
                        <Markdown>{help}</Markdown>
                    </Text>
                </TooltipIcon>
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
    iconSize: PropTypes.string,
    color: PropTypes.string,
    hoverColor: PropTypes.string,
    mono: PropTypes.bool,
};

InfoLabel.defaultProps = {
    help: null,
    inheritedFrom: null,
    iconSize: 'medium',
    color: null,
    hoverColor: null,
    mono: true,
};

export default InfoLabel;
