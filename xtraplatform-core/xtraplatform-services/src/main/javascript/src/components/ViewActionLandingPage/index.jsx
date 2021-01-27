import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import { Anchor } from 'grommet';
import { Home } from 'grommet-icons';

//TODO
const VIEW_URL = '../rest/services/';

const ViewActionLandingPage = ({ id, isOnline, parameters }) => {
    const { t } = useTranslation();

    return (
        <Anchor
            icon={<Home />}
            title={t('services/ogc_api:services.show._label')}
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
