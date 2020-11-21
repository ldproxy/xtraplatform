import React from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';
import { Tabs } from '@xtraplatform/core';

import { serviceEditTabs } from '../../constants';

const ServiceEditMain = ({ service, defaults, debounce, onPending, onChange }) => {
    const editTabs = useFassets(serviceEditTabs());

    //TODO
    const token = null;

    return (
        <Tabs
            tabs={editTabs}
            tabProps={{
                ...service,
                defaults,
                inheritedLabel: 'Service Defaults',
                debounce,
                token,
                onPending,
                onChange,
            }}
        />
    );
};

ServiceEditMain.displayName = 'ServiceEditMain';

ServiceEditMain.propTypes = {
    compact: PropTypes.bool,
    role: PropTypes.string,
};

export default ServiceEditMain;
