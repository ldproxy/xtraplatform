import React from 'react';
import PropTypes from 'prop-types';
import { gql, useQuery } from '@apollo/client';

import { Content } from '@xtraplatform/core';
import ServiceIndexHeader from './Header';
import ServiceIndexMain from './Main';

const SERVICES = gql`
  query {
    services(all: true) @rest(type: "[ServiceStatus]", path: "/services?{args}") {
      id
      lastModified
      serviceType
      label
      description
      shouldStart
      status
      hasBackgroundTask
      progress
      message
    }
  }
`;

const ServiceIndex = ({ isCompact }) => {
  const serviceTypes = [{ id: 'ogc_api', label: 'OGC API' }];
  const services = [{ id: 'flur', label: 'Flurstuecke' }];

  // TODO: error handling, on 401 delete cookie and go to root
  // TODO: set pollInterval when service is being added
  const { loading, error, data } = useQuery(SERVICES);
  console.log(loading, error, data);

  return (
    <Content
      header={
        <ServiceIndexHeader serviceTypes={serviceTypes} isCompact={isCompact} />
            }
      main={
                loading ? null : <ServiceIndexMain services={data.services} isCompact={isCompact} />
            }
    />
  );
};

ServiceIndex.displayName = 'ServiceIndex';

ServiceIndex.propTypes = {
  isCompact: PropTypes.bool,
};

export default ServiceIndex;
