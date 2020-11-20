import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

const TileGrid = ({ children, compact, ...rest }) => {
    return (
        <Box
            direction={compact ? 'column' : 'row'}
            wrap={!compact}
            justify='start'
            alignContent='start'
            pad={{
                vertical: 'xsmall',
                horizontal: compact ? 'small' : 'xsmall',
            }}
            flex='grow'
            basis='auto'
            {...rest}>
            {children}
        </Box>
    );
};

TileGrid.displayName = 'TileGrid';

TileGrid.propTypes = {
    children: PropTypes.oneOfType([PropTypes.element, PropTypes.arrayOf(PropTypes.element)]),
    compact: PropTypes.bool,
};

TileGrid.defaultProps = {
    children: null,
    compact: false,
};

export default TileGrid;
