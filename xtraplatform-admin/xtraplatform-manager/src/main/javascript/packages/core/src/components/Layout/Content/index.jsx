import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import HeaderContainer from './Header';
import MainContainer from './Main';

const Content = ({ header, main }) => {
    return (
        <Box flex fill='vertical'>
            <HeaderContainer>{header}</HeaderContainer>
            <MainContainer>{main}</MainContainer>
        </Box>
    );
};

Content.displayName = 'Content';

Content.propTypes = {
    header: PropTypes.element,
    main: PropTypes.element,
};

Content.defaultProps = {
    header: null,
    main: null,
};

export default Content;
