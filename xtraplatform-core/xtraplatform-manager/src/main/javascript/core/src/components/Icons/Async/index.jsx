import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import { Blank, StatusGoodSmall } from 'grommet-icons';

import Spinner from '../Spinner';
import Status from '../Status';

const AsyncIcon = ({ pending, loading, success, error, size }) =>
    loading ? (
        <Box>
            <Spinner size={size} color='active' />
        </Box>
    ) : pending ? (
        <Box title='Unsaved changes'>
            <StatusGoodSmall size={size} color='active' />
        </Box>
    ) : error ? (
        <Box title={error}>
            <Status size={size} value='critical' />
        </Box>
    ) : success ? (
        <Box flex={false} animation={{ type: 'fadeOut', duration: 2000, delay: 1000 }}>
            <Status value='ok' size={size} />
        </Box>
    ) : (
        <Box>
            <Blank size={size} />
        </Box>
    );

AsyncIcon.displayName = 'AsyncIcon';

AsyncIcon.propTypes = {
    size: PropTypes.string,
};

AsyncIcon.defaultProps = {
    size: 'medium',
};

export default AsyncIcon;
