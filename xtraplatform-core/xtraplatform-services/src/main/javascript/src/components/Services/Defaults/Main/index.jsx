import React from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';

import { Tabs } from '@xtraplatform/core';
import { serviceEditTabs } from '../../constants';

const ServiceDefaultsMain = ({ service, debounce, onPending, onChange }) => {
    const editTabs = useFassets(serviceEditTabs()).filter((tab) => !tab.noDefaults);
    //TODO
    const token = null;

    return (
        <Tabs
            tabs={editTabs}
            tabProps={{ ...service, token, debounce, onPending, onChange, isDefaults: true }}
        />
    );
};

ServiceDefaultsMain.displayName = 'ServiceDefaultsMain';

ServiceDefaultsMain.propTypes = {};

export default ServiceDefaultsMain;
