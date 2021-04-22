import React from 'react';
import PropTypes from 'prop-types';
import { useHistory, useLocation } from 'react-router-dom';

import { Box, Text, Heading } from 'grommet';
import { Tile, StatusIcon, SpinnerIcon, TaskProgress } from '@xtraplatform/core';

const getStatusIconType = status => {
    switch (status) {
        case "ACTIVE":    
        case "RELOADING":              
            return "ok"
        case "DISABLED":            
            return "disabled"
        case "DEFECTIVE":            
            return "critical"
        case "LOADING":           
            return "transit"    
        default:
            return "unknown";
    }
}

const getStatusText = status => {
    switch (status) {
        case "ACTIVE":            
            return "Online"
        case "DISABLED":            
            return "Offline"
        case "DEFECTIVE":            
            return "Defective"
        case "LOADING":         
            return "Initializing"
        case "RELOADING":            
            return "Reloading"    
        default:
            return "Unknown";
    }
}

const ServiceIndexMainTile = ({
    id,
    label,
    enabled,
    status,
    message,
    progress,
    hasProgress,
    hasBackgroundTask,
    isSelected,
    isCompact,
}) => {
    if (process.env.NODE_ENV !== 'production') {
        console.log('MSG', message, progress, hasBackgroundTask);
    }

    // TODO: define somewhere (json or graphql schema?)
    const isInitializing = status === 'INITIALIZING';
    const iconSize = isCompact ? 'list' : 'medium';

    // TODO: define somewhere else
    const route = '/services';
    const history = useHistory();
    const location = useLocation();

    const taskProgress = hasBackgroundTask ? (
        <TaskProgress hasProgress={hasProgress} progress={progress} message={message} iconSize={iconSize} />
    ) : null;
    const statusText = getStatusText(status);
    const statusIcon = (
        <StatusIcon
            value={getStatusIconType(status)}
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
            hoverBorderSize='large'
            pad='none'>
            {/* Card */}
            <Box fill='horizontal' textSize='small' pad="small">
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
            </Box>
            {!isCompact && taskProgress && (
                    <Box direction='row' justify='between' align='center' fill='horizontal' background='light-2' pad='small'>
                        {taskProgress}
                    </Box>
                )}
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
