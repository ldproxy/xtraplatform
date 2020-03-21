import React from 'react';
import PropTypes from 'prop-types';

import { Box, Layer } from 'grommet';

const NavigationContainer = ({ isLayer, onClose, children }) => {

    if (isLayer) {
        return (
            <Layer
                full="vertical"
                position="left"
                plain={false}
                animate
                onClickOutside={onClose}
                onEsc={onClose}
            >
                {children}
            </Layer>
        );
    }

    return (
        <Box fill="vertical" basis="1/4">
            {children}
        </Box>
    );

};

NavigationContainer.displayName = 'NavigationContainer';

NavigationContainer.propTypes = {
    isLayer: PropTypes.bool,
    onClose: PropTypes.func,
};

export default NavigationContainer;
