import React, { useCallback } from 'react';
import { Tabs } from '@xtraplatform/core';
import Main from '../Main';
import Action from '../../../Action';
import Configuration from '../../../Configuration';

const TabsOption = ({ currentID, healthchecks, selectedChecks }) => {
    const tabs = [
        { id: 'tab1', label: 'Health', component: Main },
        { id: 'tab2', label: 'Action', component: Action },
        { id: 'tab3', label: 'Configuration', component: Configuration },
    ];

    return (
        <>
            <Tabs
                tabs={tabs}
                tabProps={{
                    currentID,
                    healthchecks,
                    selectedChecks,
                }}
            />
        </>
    );
};

export default TabsOption;