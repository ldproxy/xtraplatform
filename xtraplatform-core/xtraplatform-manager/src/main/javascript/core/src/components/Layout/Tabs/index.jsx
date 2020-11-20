import React from 'react';
import PropTypes from 'prop-types';
import { useLocation, useHistory } from 'react-router-dom';
import { useQuery } from '@xtraplatform/core';

import { Box, Tabs as Tabs2, Tab } from 'grommet';

const Tabs = ({ tabs, tabProps }) => {
    const urlQuery = useQuery();
    const location = useLocation();
    const history = useHistory();

    const selectedTab = urlQuery.tab
        ? Math.max(
              tabs.findIndex((tab) => tab.id === urlQuery.tab),
              0
          )
        : 0;
    const onTabSelect = (tab) => {
        history.push({
            ...location,
            search: `?tab=${tab}`,
        });
    };

    return (
        <Tabs2
            justify='start'
            margin={{ top: 'small' }}
            fill='vertical'
            activeIndex={selectedTab}
            onActive={(index) => onTabSelect(tabs[index].id)}>
            {tabs &&
                tabs.map((tab) => {
                    const TabContent = tab.component;
                    return (
                        <Tab title={tab.label} key={tab.id} focusIndicator={false}>
                            <Box fill overflow={{ vertical: 'auto' }}>
                                <TabContent {...tabProps} />
                            </Box>
                        </Tab>
                    );
                })}
        </Tabs2>
    );
};

Tabs.displayName = 'Tabs';

Tabs.propTypes = {
    tabs: PropTypes.array,
    tabProps: PropTypes.object,
};

Tabs.defaultProps = {
    tabs: [],
    tabProps: {},
};

export default Tabs;
