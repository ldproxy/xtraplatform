import React from 'react';
import PropTypes from 'prop-types';

import { Anchor } from 'grommet';
import { Home } from 'grommet-icons';

//TODO
const VIEW_URL = '../rest/services/';

const ViewActionLandingPage = ({ id, isOnline, parameters }) => {
    return (
        <Anchor
            icon={<Home />}
            title='Show landing page'
            href={`${VIEW_URL}${id}/${parameters}`}
            target='_blank'
            disabled={!isOnline}
        />
    );
};

ViewActionLandingPage.propTypes = {};

ViewActionLandingPage.defaultProps = {};

ViewActionLandingPage.displayName = 'ViewActionLandingPage';

export default ViewActionLandingPage;
