import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import { SpinnerIcon } from '../Icons';

const Async = ({ loading, error, noSpinner, children }) => {
    if (loading) {
        return (
            <Box direction='row' align='center' justify='center' fill>
                {noSpinner || <SpinnerIcon size='large' />}
            </Box>
        );
    }

    if (error) {
        return (
            <Box direction='row' align='center' justify='center' fill>
                <Box>ERROR: {error.message}</Box>
            </Box>
        );
    }

    return children;
};

Async.displayName = 'Async';

Async.propTypes = {
    loading: PropTypes.bool.isRequired,
    error: PropTypes.shape({ message: PropTypes.string }),
    noSpinner: PropTypes.bool,
    children: PropTypes.element,
};

Async.defaultProps = {
    error: null,
    noSpinner: false,
    children: null,
};

export default Async;
