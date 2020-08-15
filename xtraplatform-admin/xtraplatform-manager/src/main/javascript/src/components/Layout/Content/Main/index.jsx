import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

const ContentMain = ({ children }) => {
    return (
        <Box fill={true}>
            {children}
        </Box>
    );
};

ContentMain.displayName = 'ContentMain';

ContentMain.propTypes = {
};

export default ContentMain;
