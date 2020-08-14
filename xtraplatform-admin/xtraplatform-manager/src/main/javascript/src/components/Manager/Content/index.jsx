import React from 'react';
import PropTypes from 'prop-types';

import { Box } from 'grommet';
import HeaderContainer from './Header';
import MainContainer from './Main';

const Content = ({ Header, Main }) => {
    return (
        <Box fill={true}>
            <HeaderContainer>
                <Header />
            </HeaderContainer>
            <MainContainer>
                <Main />
            </MainContainer>
        </Box>
    );
};

Content.displayName = 'Content';

Content.propTypes = {
    path: PropTypes.string,
    Header: PropTypes.func,
    Main: PropTypes.func,
};

export default Content;
