import React from 'react';
import PropTypes from 'prop-types';

import { Box, Layer } from 'grommet';

const Sidebar = ({ isLayer, onClose, hideBorder, children }) => {
    if (isLayer) {
        return (
            <Layer
                full='vertical'
                position='left'
                plain={false}
                animate
                onClickOutside={onClose}
                onEsc={onClose}>
                {children}
            </Layer>
        );
    }

    return (
        <Box
            fill='vertical'
            basis='1/4'
            border={
                !hideBorder && {
                    side: 'right',
                    size: 'small',
                    color: 'light-4',
                }
            }>
            {children}
        </Box>
    );
};

Sidebar.displayName = 'Sidebar';

Sidebar.propTypes = {
    isLayer: PropTypes.bool,
    onClose: PropTypes.func,
    hideBorder: PropTypes.bool,
    children: PropTypes.element,
};

Sidebar.defaultProps = {
    isLayer: false,
    onClose: null,
    hideBorder: false,
    children: null,
};

export default Sidebar;
