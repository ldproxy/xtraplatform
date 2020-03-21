import React from 'react';
import PropTypes from 'prop-types';
import { Route } from 'react-router-dom';

import { Box } from 'grommet';
import HeaderContainer from './Header';
import MainContainer from './Main';

const Content = ({ path, Header, Main }) => {
    return (
        <Route path={path} >
            <Box fill={true}>
                <HeaderContainer>
                    <Header />
                </HeaderContainer>
                <MainContainer>
                    <Main />
                </MainContainer>
            </Box>
        </Route>
    );
};

Content.displayName = 'Content';

Content.propTypes = {
    path: PropTypes.string,
    Header: PropTypes.func,
    Main: PropTypes.func,
};

export default Content;
