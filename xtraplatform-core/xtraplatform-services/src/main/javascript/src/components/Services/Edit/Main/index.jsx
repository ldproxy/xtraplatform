import React from 'react';
import PropTypes from 'prop-types';
import { useFassets } from 'feature-u';
import { Tabs } from '@xtraplatform/core';
import { useTranslation } from 'react-i18next';

import { serviceEditTabs } from '../../constants';

const ServiceEditMain = ({ service, defaults, debounce, onPending, onChange }) => {
    const editTabs = useFassets(serviceEditTabs()).sort((a, b) => a.sortPriority - b.sortPriority);
    const { t } = useTranslation();

    //TODO
    const token = null;

    return (
        <Tabs
            tabs={editTabs}
            tabProps={{
                ...service,
                defaults,
                inheritedLabel: t('services/ogc_api:services.defaults._label'),
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
