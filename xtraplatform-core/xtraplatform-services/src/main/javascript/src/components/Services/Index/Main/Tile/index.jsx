import React from 'react';
import PropTypes from 'prop-types';
import { useHistory, useLocation } from 'react-router-dom';

import { Box, Text, Heading } from 'grommet';
import { Tile, StatusIcon, SpinnerIcon, TaskProgress } from '@xtraplatform/core';

const ServiceIndexMainTile = ({
    id,
    label,
    enabled,
    status,
    message,
    progress,
    hasBackgroundTask,
    isSelected,
    isCompact,
}) => {
    if (process.env.NODE_ENV !== 'production') {
        console.log('MSG', message, progress, hasBackgroundTask);
    }

    // TODO: define somewhere (json or graphql schema?)
    const isInitializing = status === 'INITIALIZING';
    const isOnline = status === 'STARTED';
    const isDisabled = !isOnline && enabled;
    const iconSize = isCompact ? 'list' : 'medium';

    // TODO: define somewhere else
    const route = '/services';
    const history = useHistory();
    const location = useLocation();

    const taskProgress = hasBackgroundTask ? (
        <TaskProgress progress={progress} message={message} />
    ) : null;
    const statusText = isInitializing
        ? 'Initializing'
        : isOnline
        ? 'Published'
        : isDisabled
        ? 'Defective'
        : 'Offline';
    const statusIcon = isInitializing ? (
        <SpinnerIcon size={iconSize} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
    ) : (
        <StatusIcon
            value={isOnline ? 'ok' : isDisabled ? 'critical' : 'disabled'}
            size={iconSize}
            a11yTitle={statusText}
            title={statusText}
        />
    );

    return (
        <Tile
            align='start'
            direction='column'
            basis={isCompact ? 'auto' : '1/3'}
            fill={isCompact ? 'horizontal' : false}
            onClick={() => history.push({ pathname: `${route}/${id}`, search: location.search })}
            selected={isSelected}
            focusIndicator={false}
            background='background-front'
            hoverStyle='border'
            hoverColorIndex='accent-1'
            hoverBorderSize='large'>
            {/* Card */}
            <Box fill='horizontal' textSize='small'>
                <Box direction='row' justify='between' align='center' fill='horizontal'>
                    <Text
                        size={isCompact ? 'xsmall' : 'small'}
                        weight='bold'
                        color='dark-4'
                        truncate
                        title={id}
                        margin={{ right: 'xsmall' }}
                        style={{ fontFamily: '"Roboto Mono", monospace' }}>
                        {id}
                    </Text>
                    <span title={statusText}>{statusIcon}</span>
                </Box>
                {!isCompact ? (
                    <Box
                        margin={{ top: 'small' }}
                        direction='row'
                        align='center'
                        justify='between'
                        textSize='small'>
                        <Heading level='4' truncate margin='none' title={label}>
                            {label}
                        </Heading>
                    </Box>
                ) : (
                    <Box margin={{ top: 'none' }} direction='row' align='center' justify='between'>
                        <Heading level='6' truncate margin='none' title={label}>
                            {label}
                        </Heading>
                    </Box>
                )}
                {!isCompact && (
                    <Box direction='row' justify='between' align='center'>
                        {hasBackgroundTask ? (
                            <span>
                                <span style={{ verticalAlign: 'middle' }}>{taskProgress}</span>
                            </span>
                        ) : (
                            ''
                        )}
                    </Box>
                )}
            </Box>
        </Tile>
    );
};

ServiceIndexMainTile.displayName = 'ServiceIndexMainTile';

ServiceIndexMainTile.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    enabled: PropTypes.bool.isRequired,
    status: PropTypes.oneOf(['INITIALIZING', 'STARTED', 'STOPPED']),
    message: PropTypes.string,
    progress: PropTypes.number,
    hasBackgroundTask: PropTypes.bool,
    isSelected: PropTypes.bool,
    isCompact: PropTypes.bool,
};

ServiceIndexMainTile.defaultProps = {
    status: 'UNKNOWN',
    message: '',
    progress: 0,
    hasBackgroundTask: false,
    isSelected: false,
    isCompact: false,
};

export default ServiceIndexMainTile;
