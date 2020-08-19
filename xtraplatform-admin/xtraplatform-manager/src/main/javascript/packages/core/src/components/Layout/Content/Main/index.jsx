import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

const ContentMain = ({ children }) => {
    return <Box fill>{children}</Box>;
};

ContentMain.displayName = 'ContentMain';

ContentMain.propTypes = {
    children: PropTypes.element.isRequired,
};

export default ContentMain;
