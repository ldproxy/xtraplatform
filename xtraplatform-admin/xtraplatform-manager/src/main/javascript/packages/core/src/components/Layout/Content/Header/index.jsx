import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';

const ContentHeader = ({ children }) => {
    return (
        <Box
            fill='horizontal'
            flex={false}
            pad={{ horizontal: 'small', between: 'small' }}>
            <Box
                direction='row'
                fill='horizontal'
                height='xsmall'
                gap='small'
                justify='between'
                align='center'
                alignContent='center'
                flex={false}
                border={{ side: 'bottom', size: 'small', color: 'light-4' }}
                size='large'>
                {children}
            </Box>
        </Box>
    );
};

ContentHeader.displayName = 'ContentHeader';

ContentHeader.propTypes = {
    children: PropTypes.arrayOf(PropTypes.element).isRequired,
};

export default ContentHeader;
