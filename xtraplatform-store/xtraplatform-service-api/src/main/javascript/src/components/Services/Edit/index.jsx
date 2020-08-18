import React from 'react';
import PropTypes from 'prop-types';
import { useParams } from 'react-router-dom';
import { gql, useQuery } from '@apollo/client';

import { Content } from '@xtraplatform/core';
import ServiceEditHeader from './Header';
import ServiceEditMain from './Main';

const SERVICE_STATUS = gql`
  query($id: String!) {
    status(id: $id) @rest(type: "ServiceStatus", path: "/services/{args.id}/status") {
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

const SERVICE = gql`
  query($id: String!) {
    service(id: $id) @rest(type: "Service", path: "/services/{args.id}") {
      id
      lastModified
      serviceType
      label
      description
      metadata
      api
      collections
    }
  }
`;

const ServiceEdit = ({ }) => {
  const { id } = useParams();

  const { loading, error, data } = useQuery(SERVICE_STATUS, { variables: { id } });
  console.log(loading, error && error.message, data);

  const { loading: loading2, error: error2, data: data2 } = useQuery(SERVICE, { variables: { id } });
  console.log(loading2, error2 && error2.message, data2);

  if (loading || loading2) {
    return null;
  }

  if (error) {
    return error.message;
  }

  if (error2) {
    return error2.message;
  }

  const onChange = (change) => { console.log('CHANGE', change); };

  return (
    <Content
      header={
        <ServiceEditHeader service={data.status} />
            }
      main={
        <ServiceEditMain service={data2.service} onChange={onChange} />
            }
    />
  );
};

ServiceEdit.displayName = 'ServiceEdit';

ServiceEdit.propTypes = {
  isCompact: PropTypes.bool,
};

export default ServiceEdit;
