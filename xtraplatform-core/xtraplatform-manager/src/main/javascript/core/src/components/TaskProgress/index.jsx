import React from 'react';
import PropTypes from 'prop-types';

import { Box, Meter, Text } from 'grommet';

const TaskProgress = ({ progress, message }) => (
    <Box>
        <Meter
            type='bar'
            margin={{ vertical: 'small' }}
            thickness='small'
            values={[
                {
                    value: progress,
                    color: 'brand',
                },
            ]}
        />
        <Text size='small'>{message}</Text>
    </Box>
);

TaskProgress.displayName = 'TaskProgress';

TaskProgress.propTypes = {
    progress: PropTypes.number.isRequired,
    message: PropTypes.string.isRequired,
};

TaskProgress.defaultProps = {};

export default TaskProgress;
