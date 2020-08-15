import React from 'react';
import PropTypes from 'prop-types';
import { useParams, useLocation, useHistory } from "react-router-dom";
import { useFassets } from 'feature-u'
import { useQuery } from '@xtraplatform/core'

import { Box, Tabs, Tab } from 'grommet';
import { Multiple } from 'grommet-icons';
import ServiceEditGeneral from './General';
import { serviceEditTabs } from '../../constants'


//TODO: navControl, icon
const ServiceEditMain = ({ service, onChange }) => {
  const urlQuery = useQuery();
  const location = useLocation();
  const history = useHistory();
  const extEditTabs = useFassets(serviceEditTabs());


  const editTabs = [
    {
      id: 'general',
      label: 'General',
      component: ServiceEditGeneral
    },
    {
      id: 'api',
      label: 'Api',
      component: () => <div>API</div>
    },
    ...extEditTabs
  ]
  const selectedTab = urlQuery['tab'] ? Math.max(editTabs.findIndex(tab => tab.id === urlQuery['tab']), 0) : 0;
  const onTabSelect = (tab) => {
    console.log('TAB', tab); history.push({
      ...location,
      search: `?tab=${tab}`
    })
  }

  const mappingError = null;
  const token = null;


  return (
    <Tabs justify='start' margin={{ top: 'small' }} activeIndex={selectedTab} onActive={index => onTabSelect(editTabs[index].id)}>
      {editTabs &&
        editTabs.map(tab => {
          const Edit = tab.component;
          return <Tab title={tab.label} key={tab.id} focusIndicator={false} >
            <Box fill={true} overflow={{ vertical: 'auto' }} >
              {mappingError}
              <Edit {...service} token={token} onChange={onChange} />
            </Box>
          </Tab>
        })
      }
    </Tabs>
  );
};

ServiceEditMain.displayName = 'ServiceEditMain';

ServiceEditMain.propTypes = {
  compact: PropTypes.bool,
  role: PropTypes.string
};

export default ServiceEditMain;
