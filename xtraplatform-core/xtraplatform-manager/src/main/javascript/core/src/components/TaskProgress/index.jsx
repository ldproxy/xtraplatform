import React from 'react';
import PropTypes from 'prop-types';

import { Box, Meter, Text } from 'grommet';
import {SpinnerIcon} from '../'

const TaskProgress = ({ message, hasProgress, progress, iconSize, truncate }) => (
    hasProgress 
    ? <Box fill='horizontal' align='center' title={truncate ? message : null}>
        <Text size='small' truncate={truncate}>{message}</Text>
        <Meter
            type='bar'
            round
            size='large'
            margin={{ top: 'xsmall' }}
            thickness='small'
            background='white'
            values={[
                {
                    value: progress,
                    color: 'brand',
                },
            ]}
        />
    </Box>
    : <Box direction='row' justify='between' align='center' fill='horizontal'>
        <Text size='small'>{message}</Text>
        <SpinnerIcon size='list' color='brand' /> 
    </Box>
);

TaskProgress.displayName = 'TaskProgress';

TaskProgress.propTypes = {
    progress: PropTypes.number.isRequired,
    message: PropTypes.string.isRequired,
};

TaskProgress.defaultProps = {};

export default TaskProgress;
